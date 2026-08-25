param(
    [int]$StartPort = 23306
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$root = Join-Path $installerHome 'logs\mysql-import-smoke'

if (Test-Path -LiteralPath $root) {
    Remove-DirectorySafe -Root $installerHome -Path $root
}
New-Directory -Path $root

$zip = Get-ChildItem -Path (Join-Path $installerHome 'packages') -Filter 'mysql-8.0.*-winx64.zip' -File |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($null -eq $zip) {
    Fail "MySQL ZIP package not found under $(Join-Path $installerHome 'packages')."
}

$sql = Join-Path $installerHome 'app\db\wms_opc.sql'
if (-not (Test-Path -LiteralPath $sql)) {
    Fail "Database dump not found: $sql"
}
$migrationDir = Join-Path $installerHome 'app\db\migrations'
if (-not (Test-Path -LiteralPath $migrationDir)) {
    Fail "Database migration directory not found: $migrationDir"
}

Write-Step "Extracting temporary MySQL runtime"
Expand-Archive -LiteralPath $zip.FullName -DestinationPath (Join-Path $root 'mysql-raw') -Force
$mysqlHome = (Get-ChildItem -Path (Join-Path $root 'mysql-raw') -Directory | Select-Object -First 1).FullName
$mysqld = Join-Path $mysqlHome 'bin\mysqld.exe'
$mysql = Join-Path $mysqlHome 'bin\mysql.exe'
$mysqlAdmin = Join-Path $mysqlHome 'bin\mysqladmin.exe'
foreach ($file in @($mysqld, $mysql, $mysqlAdmin)) {
    if (-not (Test-Path -LiteralPath $file)) {
        Fail "Expected MySQL executable not found: $file"
    }
}

$port = Get-FreeTcpPort -StartPort $StartPort
$data = Join-Path $root 'data'
$logDir = Join-Path $root 'logs'
New-Directory -Path $data
New-Directory -Path $logDir

$myIni = Join-Path $root 'my.ini'
@"
[client]
default-character-set=utf8mb4
port=$port

[mysqld]
basedir=$(Convert-ToConfigPath $mysqlHome)
datadir=$(Convert-ToConfigPath $data)
port=$port
bind-address=127.0.0.1
character-set-server=utf8mb4
collation-server=utf8mb4_0900_ai_ci
default-time-zone='+08:00'
default_authentication_plugin=mysql_native_password
log-error=$(Convert-ToConfigPath (Join-Path $logDir 'mysql-error.log'))
"@ | Set-Content -LiteralPath $myIni -Encoding ASCII

Write-Step "Initializing temporary MySQL data directory"
Invoke-ProcessChecked -FilePath $mysqld `
    -Arguments @("--defaults-file=$myIni", '--initialize-insecure', '--console') `
    -StdOutPath (Join-Path $logDir 'initialize.out.log') `
    -StdErrPath (Join-Path $logDir 'initialize.err.log') `
    -ErrorMessage 'Temporary MySQL initialization failed.'

Write-Step "Starting temporary MySQL on port $port"
$stdout = Join-Path $logDir 'mysqld.out.log'
$stderr = Join-Path $logDir 'mysqld.err.log'
$proc = Start-Process -FilePath $mysqld `
    -ArgumentList @("--defaults-file=$myIni", '--console') `
    -PassThru `
    -WindowStyle Hidden `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr

try {
    $ready = $false
    $deadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $deadline) {
        & $mysql '--protocol=tcp' '-h127.0.0.1' "-P$port" '-uroot' '-N' '-e' 'SELECT 1' *> $null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) {
        Fail "Temporary MySQL did not become ready within 60 seconds. Logs: $logDir"
    }

    Write-Step "Importing SQL dump into temporary MySQL"
    Invoke-Checked -FilePath $mysql -Arguments @('--protocol=tcp', '-h127.0.0.1', "-P$port", '-uroot', '-e', 'CREATE DATABASE wms_opc CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;') -ErrorMessage 'Create temporary database failed.'

    $importCommand = '"' + $mysql + '" --protocol=tcp -h127.0.0.1 -P' + $port + ' -uroot wms_opc < "' + $sql + '"'
    Invoke-CmdChecked -Command $importCommand -ErrorMessage "SQL import failed: $sql"

    Write-Step "Simulating an older installed schema and preserving sample data"
    $legacyFixtureSql = @"
ALTER TABLE wms_opc.plc_addr MODIFY COLUMN addr VARCHAR(10) NOT NULL DEFAULT '';
ALTER TABLE wms_opc.operation_event DROP INDEX idx_operation_event_operation_id;
ALTER TABLE wms_opc.operation_event DROP COLUMN operation_id;
INSERT INTO wms_opc.plc_addr (id, plc_id, addr, type, scanner_id)
VALUES (990001, 990002, 'D6600', 2, 990003);
INSERT INTO wms_opc.device_info (id, type, port, status, ip, name, work_line, install_seq)
VALUES
    (990002, 4, 102, 0, '192.0.2.12', 'legacy-s7-plc', 1, 1),
    (990003, 0, 15000, 0, '192.0.2.9', 'legacy-scanner', 1, 2);
INSERT INTO wms_opc.operation_event
    (event_id, code, severity, title, message, work_line, scanner_id, device_id, device_name, qr_code)
VALUES
    ('legacy-event-001', 'PLC_OFFLINE', 'INFO', 'legacy', 'migration preservation test', 1,
     990003, 990002, 'legacy-s7-plc', '[TPL_STX]LEGACY-001[TPL_ETX]');
"@
    Invoke-Checked -FilePath $mysql -Arguments @('--protocol=tcp', '-h127.0.0.1', "-P$port", '-uroot', '-e', $legacyFixtureSql) -ErrorMessage 'Create legacy migration fixture failed.'

    $migrationFiles = @(Get-ChildItem -LiteralPath $migrationDir -Filter '*.sql' -File | Sort-Object Name)
    if ($migrationFiles.Count -lt 2) {
        Fail "Expected bundled database migrations were not found: $migrationDir"
    }
    foreach ($pass in 1..2) {
        Write-Step "Applying bundled database migrations (idempotency pass $pass)"
        foreach ($migrationFile in $migrationFiles) {
            $migrationCommand = '"' + $mysql + '" --protocol=tcp -h127.0.0.1 -P' + $port + ' -uroot wms_opc < "' + $migrationFile.FullName + '"'
            Invoke-CmdChecked -Command $migrationCommand -ErrorMessage "Database migration failed: $($migrationFile.FullName)"
        }
    }

    Write-Step "Validating imported and migrated schema"
    $schemaResult = @(& $mysql '--protocol=tcp' '-h127.0.0.1' "-P$port" '-uroot' '-N' '-B' '-e' @"
SELECT CONCAT(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wms_opc'), '|',
  COALESCE((SELECT character_maximum_length FROM information_schema.columns WHERE table_schema='wms_opc' AND table_name='plc_addr' AND column_name='addr'), 0), '|',
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='wms_opc' AND table_name='operation_event' AND column_name='operation_id'), '|',
  (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema='wms_opc' AND table_name='operation_event' AND index_name='idx_operation_event_operation_id'), '|',
  (SELECT COUNT(*) FROM wms_opc.plc_addr WHERE id=990001 AND addr='D6600'), '|',
  (SELECT COUNT(*) FROM wms_opc.operation_event WHERE event_id='legacy-event-001' AND operation_id='legacy-event-001'), '|',
  (SELECT COUNT(*) FROM wms_opc.device_info WHERE id=990002 AND type=3), '|',
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='wms_opc' AND table_name='operation_event'
      AND column_name IN ('scanner_name', 'scanner_ip', 'plc_id', 'plc_name', 'plc_ip')), '|',
  (SELECT COUNT(*) FROM wms_opc.operation_event WHERE event_id='legacy-event-001'
      AND scanner_name='legacy-scanner' AND scanner_ip='192.0.2.9'
      AND plc_id=990002 AND plc_name='legacy-s7-plc' AND plc_ip='192.0.2.12'
      AND qr_code='[TPL_STX]LEGACY-001[TPL_ETX]')
);
"@)
    if ($LASTEXITCODE -ne 0) {
        Fail "Imported schema validation query failed."
    }
    if ($schemaResult.Count -ne 1) {
        Fail "Imported schema validation returned an unexpected result: $($schemaResult -join ', ')"
    }
    $schemaValues = $schemaResult[0].Trim() -split '\|'
    if ($schemaValues.Count -ne 9 -or [int]$schemaValues[0] -lt 8) {
        Fail "Imported schema is missing required tables: $($schemaResult[0])"
    }
    if ([int]$schemaValues[1] -lt 64) {
        Fail "plc_addr.addr must support at least 64 characters for Siemens S7 addresses. Result=$($schemaResult[0])"
    }
    if ([int]$schemaValues[2] -ne 1 -or [int]$schemaValues[3] -lt 1) {
        Fail "operation_event.operation_id or its index is missing. Result=$($schemaResult[0])"
    }
    if ([int]$schemaValues[4] -ne 1 -or [int]$schemaValues[5] -ne 1) {
        Fail "Bundled migrations did not preserve or backfill legacy records. Result=$($schemaResult[0])"
    }
    if ([int]$schemaValues[6] -ne 1) {
        Fail "Historical S7-1500 records were not merged into the shared S7 type. Result=$($schemaResult[0])"
    }
    if ([int]$schemaValues[7] -ne 5 -or [int]$schemaValues[8] -ne 1) {
        Fail "Operation-event device context columns or historical backfill are missing. Result=$($schemaResult[0])"
    }
    Write-Ok "Database dump import and idempotent migration smoke test passed."
} finally {
    if ($null -ne $proc -and -not $proc.HasExited) {
        & $mysqlAdmin '--protocol=tcp' '-h127.0.0.1' "-P$port" '-uroot' 'shutdown' *> $null
        Start-Sleep -Seconds 2
        if (-not $proc.HasExited) {
            Stop-Process -Id $proc.Id -Force
        }
    }
}
