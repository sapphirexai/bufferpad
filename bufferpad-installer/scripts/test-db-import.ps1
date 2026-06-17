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

    Write-Step "Validating imported schema"
    & $mysql '--protocol=tcp' '-h127.0.0.1' "-P$port" '-uroot' '-N' '-e' "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wms_opc'; SELECT COUNT(*) FROM wms_opc.opc_config;"
    if ($LASTEXITCODE -ne 0) {
        Fail "Imported schema validation query failed."
    }
    Write-Ok "Database dump import smoke test passed."
} finally {
    if ($null -ne $proc -and -not $proc.HasExited) {
        & $mysqlAdmin '--protocol=tcp' '-h127.0.0.1' "-P$port" '-uroot' 'shutdown' *> $null
        Start-Sleep -Seconds 2
        if (-not $proc.HasExited) {
            Stop-Process -Id $proc.Id -Force
        }
    }
}
