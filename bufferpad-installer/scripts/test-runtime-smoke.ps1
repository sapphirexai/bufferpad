param(
    [int]$MysqlStartPort = 23306,
    [int]$BackendStartPort = 29001,
    [int]$FrontendStartPort = 28088
)

. (Join-Path $PSScriptRoot 'common.ps1')

function New-MySqlClientFile {
    param(
        [string]$Path,
        [string]$User,
        [string]$Password,
        [int]$Port,
        [switch]$NoPassword
    )
    $lines = @(
        '[client]',
        "user=$User",
        'host=127.0.0.1',
        "port=$Port",
        'default-character-set=utf8mb4'
    )
    if (-not $NoPassword) {
        $lines += "password=$Password"
    }
    Set-Content -LiteralPath $Path -Value ($lines -join [Environment]::NewLine) -Encoding ASCII
}

function Test-MySqlConnection {
    param(
        [string]$MysqlExe,
        [string]$ClientFile
    )

    $process = $null
    try {
        $psi = New-Object Diagnostics.ProcessStartInfo
        $psi.FileName = $MysqlExe
        $psi.Arguments = "--defaults-extra-file=`"$ClientFile`" -N -e `"SELECT 1`""
        $psi.UseShellExecute = $false
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError = $true
        $process = [Diagnostics.Process]::Start($psi)
        if (-not $process.WaitForExit(5000)) {
            try {
                $process.Kill()
            } catch {
            }
            return $false
        }
        return ($process.ExitCode -eq 0)
    } catch {
        return $false
    } finally {
        if ($null -ne $process) {
            $process.Dispose()
        }
    }
}

function Invoke-MySqlSql {
    param(
        [string]$MysqlExe,
        [string]$ClientFile,
        [string]$Sql,
        [string]$ErrorMessage
    )
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = $MysqlExe
    $psi.Arguments = "--defaults-extra-file=`"$ClientFile`""
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $process = [Diagnostics.Process]::Start($psi)
    $process.StandardInput.WriteLine($Sql)
    $process.StandardInput.Close()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        Fail "$ErrorMessage ExitCode=$($process.ExitCode) Error=$stderr Output=$stdout"
    }
}

$installerHome = Get-InstallerHome
$config = Read-InstallConfig -InstallerHome $installerHome -InstallRootOverride $null
Assert-SimpleSqlIdentifier -Value ([string]$config['DatabaseName']) -Name 'DatabaseName'
Assert-SimpleSqlIdentifier -Value ([string]$config['MysqlUser']) -Name 'MysqlUser'

$root = Join-Path $installerHome 'logs\runtime-smoke'
if (Test-Path -LiteralPath $root) {
    Remove-DirectorySafe -Root $installerHome -Path $root
}
New-Directory -Path $root

$mysqlPort = Get-FreeTcpPort -StartPort $MysqlStartPort
$backendPort = Get-FreeTcpPort -StartPort $BackendStartPort
$frontendPort = Get-FreeTcpPort -StartPort $FrontendStartPort

$javaZip = Find-Package -InstallerHome $installerHome -Pattern ([string]$config['JavaPackagePattern']) -DisplayName 'JDK 17'
$mysqlZip = Find-Package -InstallerHome $installerHome -Pattern ([string]$config['MysqlPackagePattern']) -DisplayName 'MySQL'
$nginxZip = Find-Package -InstallerHome $installerHome -Pattern ([string]$config['NginxPackagePattern']) -DisplayName 'nginx'
$backendJarSource = Join-Path $installerHome ("app\backend\" + [string]$config['BackendJarName'])
$frontendSource = Join-Path $installerHome 'app\frontend\dist'
$dbDumpSource = Join-Path $installerHome ("app\db\" + [string]$config['DbDumpFileName'])

foreach ($requiredPath in @($backendJarSource, (Join-Path $frontendSource 'index.html'), $dbDumpSource)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        Fail "Required runtime smoke input is missing: $requiredPath"
    }
}

$paths = @{
    Java = Join-Path $root 'java'
    Mysql = Join-Path $root 'mysql'
    Nginx = Join-Path $root 'nginx'
    Backend = Join-Path $root 'backend'
    Frontend = Join-Path $root 'frontend'
    Conf = Join-Path $root 'conf'
    Logs = Join-Path $root 'logs'
    MysqlData = Join-Path $root 'data\mysql'
    BackendLog = Join-Path $root 'logs\backend'
    NginxLog = Join-Path $root 'logs\nginx'
    ServiceLog = Join-Path $root 'logs\service'
}

$mysqlProc = $null
$backendProc = $null
$nginxProc = $null
$script:SmokeFailed = $false

function Stop-ProcessTreeInRuntimeRoot {
    param([string]$RuntimeRoot)

    $rootFull = [IO.Path]::GetFullPath($RuntimeRoot).TrimEnd('\')
    $targets = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $name = [string]$_.Name
            $tracked = $name -in @('java.exe', 'javaw.exe', 'mysqld.exe', 'nginx.exe')
            $exe = [string]$_.ExecutablePath
            $cmd = [string]$_.CommandLine
            $tracked -and (
                (-not [string]::IsNullOrWhiteSpace($exe) -and $exe.IndexOf($rootFull, [StringComparison]::OrdinalIgnoreCase) -ge 0) -or
                (-not [string]::IsNullOrWhiteSpace($cmd) -and $cmd.IndexOf($rootFull, [StringComparison]::OrdinalIgnoreCase) -ge 0)
            )
        })

    foreach ($target in $targets) {
        Stop-Process -Id $target.ProcessId -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-StopCommandWithTimeout {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [int]$TimeoutSeconds = 5
    )

    if (-not (Test-Path -LiteralPath $FilePath)) {
        return
    }
    $process = Start-Process -FilePath $FilePath -ArgumentList $Arguments -PassThru -WindowStyle Hidden
    if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    }
}

try {
    Write-Step "Preparing temporary runtime under $root"
    foreach ($dir in $paths.Values) {
        New-Directory -Path ([string]$dir)
    }
    New-Directory -Path (Join-Path $paths.Logs 'mysql')

    Expand-ZipToDirectory -ZipPath $javaZip -Destination $paths.Java -InstallRoot $root
    Expand-ZipToDirectory -ZipPath $mysqlZip -Destination $paths.Mysql -InstallRoot $root
    Expand-ZipToDirectory -ZipPath $nginxZip -Destination $paths.Nginx -InstallRoot $root
    Copy-Item -LiteralPath $backendJarSource -Destination (Join-Path $paths.Backend ([string]$config['BackendJarName'])) -Force
    Copy-Item -Path (Join-Path $frontendSource '*') -Destination $paths.Frontend -Recurse -Force

    $javaExe = Join-Path $paths.Java 'bin\java.exe'
    $mysqldExe = Join-Path $paths.Mysql 'bin\mysqld.exe'
    $mysqlExe = Join-Path $paths.Mysql 'bin\mysql.exe'
    $mysqlAdminExe = Join-Path $paths.Mysql 'bin\mysqladmin.exe'
    $nginxExe = Join-Path $paths.Nginx 'nginx.exe'
    foreach ($file in @($javaExe, $mysqldExe, $mysqlExe, $mysqlAdminExe, $nginxExe)) {
        if (-not (Test-Path -LiteralPath $file)) {
            Fail "Expected runtime executable is missing: $file"
        }
    }

    Write-Step "Rendering temporary backend and nginx config"
    $tokens = @{
        BACKEND_PORT = $backendPort
        MYSQL_PORT = $mysqlPort
        DB_NAME = [string]$config['DatabaseName']
        DB_USER = [string]$config['MysqlUser']
        DB_PASSWORD = [string]$config['MysqlPassword']
        BACKEND_LOG_FILE = Convert-ToConfigPath (Join-Path $paths.BackendLog 'wms-opc-prod.log')
        MYSQL_BASE_DIR = Convert-ToConfigPath $paths.Mysql
        MYSQL_DATA_DIR = Convert-ToConfigPath $paths.MysqlData
        MYSQL_ERROR_LOG = Convert-ToConfigPath (Join-Path $paths.Logs 'mysql\mysql-error.log')
        NGINX_PID_FILE = Convert-ToConfigPath (Join-Path $paths.NginxLog 'nginx.pid')
        NGINX_ERROR_LOG = Convert-ToConfigPath (Join-Path $paths.NginxLog 'error.log')
        NGINX_ACCESS_LOG = Convert-ToConfigPath (Join-Path $paths.NginxLog 'access.log')
        FRONTEND_PORT = $frontendPort
        FRONTEND_ROOT = Convert-ToConfigPath $paths.Frontend
    }
    $myIni = Join-Path $paths.Conf 'my.ini'
    Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\application-prod.yml.template') -DestinationPath (Join-Path $paths.Conf 'application-prod.yml') -Tokens $tokens
    Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\my.ini.template') -DestinationPath $myIni -Tokens $tokens
    Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\nginx.conf.template') -DestinationPath (Join-Path $paths.Nginx 'conf\nginx.conf') -Tokens $tokens

    Write-Step "Initializing temporary MySQL"
    Invoke-ProcessChecked -FilePath $mysqldExe `
        -Arguments @("--defaults-file=$myIni", '--initialize-insecure', '--console') `
        -StdOutPath (Join-Path $paths.Logs 'mysql\initialize.out.log') `
        -StdErrPath (Join-Path $paths.Logs 'mysql\initialize.err.log') `
        -ErrorMessage 'Temporary MySQL initialization failed.'

    Write-Step "Starting temporary MySQL on port $mysqlPort"
    $mysqlProc = Start-Process -FilePath $mysqldExe `
        -ArgumentList @("--defaults-file=$myIni", '--console') `
        -PassThru `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $paths.Logs 'mysql\mysqld.out.log') `
        -RedirectStandardError (Join-Path $paths.Logs 'mysql\mysqld.err.log')

    $clientNoPassword = Join-Path $paths.Conf 'mysql-client-nopassword.cnf'
    $clientWithPassword = Join-Path $paths.Conf 'mysql-client.cnf'
    New-MySqlClientFile -Path $clientNoPassword -User 'root' -Password '' -Port $mysqlPort -NoPassword
    New-MySqlClientFile -Path $clientWithPassword -User 'root' -Password ([string]$config['MysqlRootPassword']) -Port $mysqlPort

    $ready = $false
    $deadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $deadline) {
        if (Test-MySqlConnection -MysqlExe $mysqlExe -ClientFile $clientNoPassword) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) {
        Fail "Temporary MySQL did not become ready. Logs: $($paths.Logs)"
    }

    $dbName = [string]$config['DatabaseName']
    $rootPasswordSql = ([string]$config['MysqlRootPassword']).Replace("'", "''")
    $appUser = [string]$config['MysqlUser']
    $appPasswordSql = ([string]$config['MysqlPassword']).Replace("'", "''")
    $appUserSql = ''
    if ($appUser -ne 'root') {
        $appUserSql = @"
CREATE USER IF NOT EXISTS '$appUser'@'localhost' IDENTIFIED WITH mysql_native_password BY '$appPasswordSql';
ALTER USER '$appUser'@'localhost' IDENTIFIED WITH mysql_native_password BY '$appPasswordSql';
CREATE USER IF NOT EXISTS '$appUser'@'127.0.0.1' IDENTIFIED WITH mysql_native_password BY '$appPasswordSql';
ALTER USER '$appUser'@'127.0.0.1' IDENTIFIED WITH mysql_native_password BY '$appPasswordSql';
GRANT ALL PRIVILEGES ON $dbName.* TO '$appUser'@'localhost';
GRANT ALL PRIVILEGES ON $dbName.* TO '$appUser'@'127.0.0.1';
"@
    }
    $bootstrapSql = @"
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '$rootPasswordSql';
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED WITH mysql_native_password BY '$rootPasswordSql';
ALTER USER 'root'@'127.0.0.1' IDENTIFIED WITH mysql_native_password BY '$rootPasswordSql';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;
CREATE DATABASE IF NOT EXISTS $dbName CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
$appUserSql
FLUSH PRIVILEGES;
"@
    Invoke-MySqlSql -MysqlExe $mysqlExe -ClientFile $clientNoPassword -Sql $bootstrapSql -ErrorMessage 'Temporary MySQL bootstrap failed.'

    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $deadline) {
        if (Test-MySqlConnection -MysqlExe $mysqlExe -ClientFile $clientWithPassword) {
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not (Test-MySqlConnection -MysqlExe $mysqlExe -ClientFile $clientWithPassword)) {
        Fail "Temporary MySQL password bootstrap did not become ready."
    }

    Write-Step "Importing database dump"
    $importCommand = '"' + $mysqlExe + '" --defaults-extra-file="' + $clientWithPassword + '" ' + $dbName + ' < "' + $dbDumpSource + '"'
    Invoke-CmdChecked -Command $importCommand -ErrorMessage "Database import failed from $dbDumpSource."

    Write-Step "Starting backend on port $backendPort"
    $backendJar = Join-Path $paths.Backend ([string]$config['BackendJarName'])
    $confLocation = 'file:///' + (Convert-ToConfigPath $paths.Conf) + '/'
    $backendArgs = '-Xms128m -Xmx256m -Dfile.encoding=UTF-8 -jar "' + $backendJar + '" --spring.profiles.active=prod --spring.config.additional-location=' + $confLocation
    $backendProc = Start-Process -FilePath $javaExe `
        -ArgumentList $backendArgs `
        -PassThru `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $paths.BackendLog 'backend.out.log') `
        -RedirectStandardError (Join-Path $paths.BackendLog 'backend.err.log')
    Wait-HttpOk -Url "http://127.0.0.1:$backendPort/actuator/health" -TimeoutSeconds 120 | Out-Null

    Write-Step "Starting nginx on port $frontendPort"
    Invoke-Checked -FilePath $nginxExe -Arguments @('-p', $paths.Nginx, '-c', 'conf/nginx.conf', '-t') -ErrorMessage 'Temporary nginx config test failed.'
    $nginxProc = Start-Process -FilePath $nginxExe `
        -ArgumentList @('-p', $paths.Nginx, '-c', 'conf/nginx.conf') `
        -PassThru `
        -WindowStyle Hidden
    Start-Sleep -Seconds 2
    if ($nginxProc.HasExited -and $nginxProc.ExitCode -ne 0) {
        Fail "Temporary nginx exited immediately. ExitCode=$($nginxProc.ExitCode)"
    }

    Wait-HttpOk -Url "http://127.0.0.1:$frontendPort/" -TimeoutSeconds 30 | Out-Null
    Wait-HttpOk -Url "http://127.0.0.1:$frontendPort/api/opcConfig/page" -TimeoutSeconds 30 | Out-Null

    $deviceTypesResponse = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$frontendPort/api/options/deviceTypes" -TimeoutSec 10
    $deviceTypesPayload = $deviceTypesResponse.Content | ConvertFrom-Json
    $deviceTypeValues = @($deviceTypesPayload.data | ForEach-Object { [int]$_.value })
    foreach ($requiredType in @(3, 4)) {
        if ($deviceTypeValues -notcontains $requiredType) {
            Fail "Device type endpoint is missing Siemens PLC type $requiredType. Returned: $($deviceTypeValues -join ', ')"
        }
    }

    Write-Ok "Runtime smoke test passed."
    Write-Host "Frontend: http://127.0.0.1:$frontendPort"
    Write-Host "Backend:  http://127.0.0.1:$backendPort"
} catch {
    $script:SmokeFailed = $true
    Write-Host ""
    Write-Host $_.Exception.Message -ForegroundColor Red
} finally {
    if (Test-Path -LiteralPath (Join-Path $paths.Nginx 'nginx.exe')) {
        Invoke-StopCommandWithTimeout -FilePath (Join-Path $paths.Nginx 'nginx.exe') -Arguments @('-p', $paths.Nginx, '-s', 'stop') -TimeoutSeconds 5
    }
    if ($null -ne $backendProc -and -not $backendProc.HasExited) {
        Stop-Process -Id $backendProc.Id -Force -ErrorAction SilentlyContinue
    }
    if ($null -ne $mysqlProc -and -not $mysqlProc.HasExited) {
        if (Test-Path -LiteralPath (Join-Path $paths.Mysql 'bin\mysqladmin.exe')) {
            Invoke-StopCommandWithTimeout -FilePath (Join-Path $paths.Mysql 'bin\mysqladmin.exe') -Arguments @("--defaults-extra-file=$clientWithPassword", 'shutdown') -TimeoutSeconds 5
        }
        Start-Sleep -Seconds 2
        if (-not $mysqlProc.HasExited) {
            Stop-Process -Id $mysqlProc.Id -Force -ErrorAction SilentlyContinue
        }
    }
    Stop-ProcessTreeInRuntimeRoot -RuntimeRoot $root
}

if ($script:SmokeFailed) {
    exit 1
}

exit 0
