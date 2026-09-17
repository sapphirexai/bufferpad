param(
    [string]$InstallRoot,
    [string]$SqlFile
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$config = Read-InstallConfig -InstallerHome $installerHome -InstallRootOverride $InstallRoot
$root = [string]$config['InstallRoot']

if ([string]::IsNullOrWhiteSpace($SqlFile)) {
    $SqlFile = Join-Path $installerHome ("app\db\" + [string]$config['DbDumpFileName'])
}
if (-not (Test-Path -LiteralPath $SqlFile)) {
    Fail "SQL file not found: $SqlFile"
}

$mysqlExe = Join-Path $root 'mysql\bin\mysql.exe'
if (-not (Test-Path -LiteralPath $mysqlExe)) {
    Fail "mysql.exe not found: $mysqlExe. Run scripts\install.ps1 first."
}

$clientFile = Join-Path $root 'conf\mysql-client.cnf'
if (-not (Test-Path -LiteralPath $clientFile)) {
    New-Directory -Path (Split-Path -Parent $clientFile)
    Set-Content -LiteralPath $clientFile -Encoding ASCII -Value @(
        '[client]',
        'user=root',
        "password=$([string]$config['MysqlRootPassword'])",
        'host=127.0.0.1',
        "port=$([int]$config['MysqlPort'])",
        'default-character-set=utf8mb4'
    )
}

$dbName = [string]$config['DatabaseName']
Assert-SimpleSqlIdentifier -Value $dbName -Name 'DatabaseName'
Write-Step "Importing $SqlFile into $dbName"
$command = '"' + $mysqlExe + '" --defaults-extra-file="' + $clientFile + '" ' + $dbName + ' < "' + $SqlFile + '"'
Invoke-CmdChecked -Command $command -ErrorMessage 'Database import failed.'
Write-Ok "Database import completed."
