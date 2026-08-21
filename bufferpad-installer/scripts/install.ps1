param(
    [string]$InstallRoot,
    [switch]$Force,
    [switch]$ResetData,
    [switch]$DryRun,
    [switch]$SkipPackageDownload,
    [switch]$SkipDbImport,
    [switch]$NoFirewallRule,
    [int]$FrontendPort,
    [int]$BackendPort,
    [int]$MysqlPort
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$logDir = Join-Path $installerHome 'logs'
New-Directory -Path $logDir
$transcriptPath = Join-Path $logDir ("install-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + ".log")

try {
    Start-Transcript -Path $transcriptPath -Force | Out-Null
} catch {
    Write-Warn "Could not start transcript: $($_.Exception.Message)"
}

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

function Wait-MySqlConnection {
    param(
        [string]$MysqlExe,
        [string]$ClientFile,
        [int]$TimeoutSeconds = 60
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-MySqlConnection -MysqlExe $MysqlExe -ClientFile $ClientFile) {
            return
        }
        Start-Sleep -Seconds 2
    }
    Fail "MySQL did not become ready within $TimeoutSeconds seconds."
}

function Test-InstallerPackagesReady {
    param(
        [string]$InstallerHome,
        [hashtable]$Config
    )

    $requirements = @(
        @('JDK 17', [string]$Config['JavaPackagePattern']),
        @('MySQL', [string]$Config['MysqlPackagePattern']),
        @('nginx', [string]$Config['NginxPackagePattern']),
        @('WinSW', [string]$Config['WinSWPackagePattern']),
        @('VC++ Redistributable', [string]$Config['VCRedistPattern'])
    )

    $missing = New-Object System.Collections.Generic.List[string]
    foreach ($requirement in $requirements) {
        $pattern = [string]$requirement[1]
        $match = @(Get-ChildItem -Path (Join-Path $InstallerHome 'packages') -Filter $pattern -File -ErrorAction SilentlyContinue | Select-Object -First 1)
        if ($match.Count -eq 0) {
            $missing.Add("$($requirement[0]) ($pattern)")
        }
    }
    return [pscustomobject]@{
        Ready = ($missing.Count -eq 0)
        Missing = @($missing)
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

function Set-BufferPadServiceAutoStart {
    param(
        [string]$ServiceName,
        [switch]$Delayed
    )

    Set-Service -Name $ServiceName -StartupType Automatic

    $registryPath = "HKLM:\SYSTEM\CurrentControlSet\Services\$ServiceName"
    if (Test-Path -LiteralPath $registryPath) {
        $delayedValue = if ($Delayed) { 1 } else { 0 }
        $delayedProperty = Get-ItemProperty -LiteralPath $registryPath -Name DelayedAutoStart -ErrorAction SilentlyContinue
        if ($null -eq $delayedProperty) {
            New-ItemProperty -LiteralPath $registryPath -Name DelayedAutoStart -PropertyType DWord -Value $delayedValue -Force | Out-Null
        } else {
            Set-ItemProperty -LiteralPath $registryPath -Name DelayedAutoStart -Value $delayedValue
        }
    }

    $escapedName = $ServiceName.Replace("'", "''")
    $service = Get-CimInstance -ClassName Win32_Service -Filter "Name='$escapedName'" -ErrorAction Stop
    if ($service.StartMode -ne 'Auto') {
        Fail "Service $ServiceName startup type is not Automatic. Current StartMode: $($service.StartMode)"
    }

    if ($Delayed) {
        $delayedAutoStart = 0
        if (Test-Path -LiteralPath $registryPath) {
            $property = Get-ItemProperty -LiteralPath $registryPath -Name DelayedAutoStart -ErrorAction SilentlyContinue
            if ($null -ne $property) {
                $delayedAutoStart = [int]$property.DelayedAutoStart
            }
        }
        if ($delayedAutoStart -ne 1) {
            Fail "Service $ServiceName is Automatic but delayed auto-start was not enabled."
        }
        Write-Ok "$ServiceName startup type is Automatic (Delayed)."
    } else {
        Write-Ok "$ServiceName startup type is Automatic."
    }
}

function Write-BackendManagementScripts {
    param(
        [string]$InstallerHome,
        [string]$DestinationDir,
        [hashtable]$Tokens
    )

    New-Directory -Path $DestinationDir
    foreach ($scriptName in @(
        'backend-prod-monitor.ps1',
        'backend-prod-monitor-start.ps1',
        'backend-prod-monitor-stop.ps1',
        'backend-prod-start-once.ps1',
        'backend-prod-stop-once.ps1',
        'backend-script-wrapper.ps1',
        'backend-prod-monitor-start.cmd',
        'backend-prod-monitor-stop.cmd',
        'backend-prod-start-once.cmd',
        'backend-prod-stop-once.cmd'
    )) {
        Write-RenderedTemplate `
            -TemplatePath (Join-Path $InstallerHome "config\$scriptName.template") `
            -DestinationPath (Join-Path $DestinationDir $scriptName) `
            -Tokens $Tokens
    }
}

try {
    $config = Read-InstallConfig -InstallerHome $installerHome -InstallRootOverride $InstallRoot
    if ($PSBoundParameters.ContainsKey('FrontendPort')) { $config['FrontendPort'] = $FrontendPort }
    if ($PSBoundParameters.ContainsKey('BackendPort')) { $config['BackendPort'] = $BackendPort }
    if ($PSBoundParameters.ContainsKey('MysqlPort')) { $config['MysqlPort'] = $MysqlPort }
    $root = [string]$config['InstallRoot']

    $existingMysqlData = Join-Path $root 'data\mysql'
    $hasExistingMysqlData = (Test-Path -LiteralPath $existingMysqlData) -and
        (@(Get-ChildItem -LiteralPath $existingMysqlData -Force -ErrorAction SilentlyContinue).Count -gt 0)
    if ($Force -and -not $ResetData -and $hasExistingMysqlData -and -not $SkipDbImport) {
        $SkipDbImport = $true
        Write-Warn "Existing MySQL data detected. Database dump import is disabled for this update; bundled migrations will still run."
    }

    $paths = @{
        Root = $root
        Java = Join-Path $root 'java'
        Mysql = Join-Path $root 'mysql'
        Nginx = Join-Path $root 'nginx'
        Backend = Join-Path $root 'backend'
        Frontend = Join-Path $root 'frontend'
        Conf = Join-Path $root 'conf'
        Service = Join-Path $root 'service'
        Logs = Join-Path $root 'logs'
        MysqlData = Join-Path $root 'data\mysql'
        BackendLog = Join-Path $root 'logs\backend'
        NginxLog = Join-Path $root 'logs\nginx'
        ServiceLog = Join-Path $root 'logs\service'
    }

    $mysqlServiceName = [string]$config['MysqlServiceName']
    $backendServiceName = [string]$config['BackendServiceName']
    $nginxServiceName = [string]$config['NginxServiceName']
    $backendMonitorTaskName = [string]$config['BackendMonitorTaskName']
    $backendMonitorIntervalSeconds = [int]$config['BackendMonitorIntervalSeconds']
    $backendPort = [int]$config['BackendPort']
    $frontendPort = [int]$config['FrontendPort']
    $mysqlPort = [int]$config['MysqlPort']
    Assert-SimpleSqlIdentifier -Value ([string]$config['DatabaseName']) -Name 'DatabaseName'
    Assert-SimpleSqlIdentifier -Value ([string]$config['MysqlUser']) -Name 'MysqlUser'

    Write-Step "BufferPad Windows installer plan"
    Write-Host "Installer: $installerHome"
    Write-Host "Install root: $root"
    Write-Host "Ports: MySQL=$mysqlPort Backend=$backendPort Frontend=$frontendPort"
    Write-Host "Services: $mysqlServiceName, $backendServiceName, $nginxServiceName"
    Write-Host "Backend monitor task: $backendMonitorTaskName"

    if ($DryRun) {
        Write-Warn "DryRun mode: no system changes will be made."
    } else {
        Assert-Admin
    }

    $packageCheck = Test-InstallerPackagesReady -InstallerHome $installerHome -Config $config
    if (-not $packageCheck.Ready -and -not $SkipPackageDownload -and -not $DryRun) {
        Write-Warn "Missing package(s): $($packageCheck.Missing -join ', ')"
        Write-Step "Trying to download missing runtime packages"
        & (Join-Path $PSScriptRoot 'download-packages.ps1')
    }

    Write-Step "Validating installer inputs"
    $backendJarSource = Join-Path $installerHome ("app\backend\" + [string]$config['BackendJarName'])
    $frontendSource = Join-Path $installerHome 'app\frontend\dist'
    $dbDumpSource = Join-Path $installerHome ("app\db\" + [string]$config['DbDumpFileName'])
    $dbMigrationDir = Join-Path $installerHome 'app\db\migrations'

    if (-not (Test-Path -LiteralPath $backendJarSource)) {
        Fail "Backend jar is missing: $backendJarSource. Run scripts\prepare-app.ps1 first."
    }
    if (-not (Test-Path -LiteralPath (Join-Path $frontendSource 'index.html'))) {
        Fail "Frontend dist is missing: $frontendSource. Run scripts\prepare-app.ps1 first."
    }
    if (-not $SkipDbImport -and -not (Test-Path -LiteralPath $dbDumpSource)) {
        Fail "Database dump is missing: $dbDumpSource. Put your exported SQL there or run install.ps1 -SkipDbImport for a temporary test."
    }
    if (-not (Test-Path -LiteralPath $dbMigrationDir)) {
        Fail "Database migration directory is missing: $dbMigrationDir"
    }

    $javaZip = $null
    $mysqlZip = $null
    $nginxZip = $null
    $winswExe = $null
    $vcRedist = $null
    if (-not $DryRun) {
        $javaZip = Find-Package -InstallerHome $installerHome -Pattern ([string]$config['JavaPackagePattern']) -DisplayName 'JDK 17'
        $mysqlZip = Find-Package -InstallerHome $installerHome -Pattern ([string]$config['MysqlPackagePattern']) -DisplayName 'MySQL'
        $nginxZip = Find-Package -InstallerHome $installerHome -Pattern ([string]$config['NginxPackagePattern']) -DisplayName 'nginx'
        $winswExe = Find-Package -InstallerHome $installerHome -Pattern ([string]$config['WinSWPackagePattern']) -DisplayName 'WinSW'
        $vcRedist = Find-Package -InstallerHome $installerHome -Pattern ([string]$config['VCRedistPattern']) -DisplayName 'VC++ Redistributable'
    } else {
        foreach ($item in @(
            @('JDK 17', [string]$config['JavaPackagePattern']),
            @('MySQL', [string]$config['MysqlPackagePattern']),
            @('nginx', [string]$config['NginxPackagePattern']),
            @('WinSW', [string]$config['WinSWPackagePattern']),
            @('VC++ Redistributable', [string]$config['VCRedistPattern'])
        )) {
            $pkg = @(Get-ChildItem -Path (Join-Path $installerHome 'packages') -Filter $item[1] -File -ErrorAction SilentlyContinue)
            if ($pkg.Count -eq 0) {
                Write-Warn "$($item[0]) package is not present yet. Run scripts\download-packages.ps1 before a full install."
            }
        }
    }

    Write-Step "Checking ports and existing services"
    if (-not $DryRun) {
        if (-not $Force) {
            foreach ($serviceName in @($nginxServiceName, $backendServiceName, $mysqlServiceName)) {
                if (Get-Service -Name $serviceName -ErrorAction SilentlyContinue) {
                    Fail "Service $serviceName already exists. Use -Force to update an existing BufferPad installation."
                }
            }
            if (Test-Path -LiteralPath $root) {
                Fail "Install root already exists: $root. Use -Force to update it, or choose another InstallRoot."
            }
        } else {
            Stop-BufferPadBackendMonitor `
                -TaskName $backendMonitorTaskName `
                -MonitorScript (Join-Path $paths.Backend 'backend-prod-monitor.ps1') `
                -StopFile (Join-Path $paths.Backend '.backend-prod-monitor.stop') `
                -Root $root
            Remove-WinSWServiceIfExists -ServiceName $nginxServiceName -ExePath (Join-Path $paths.Service "$nginxServiceName.exe")
            Remove-WinSWServiceIfExists -ServiceName $backendServiceName -ExePath (Join-Path $paths.Service "$backendServiceName.exe")
            Remove-NativeServiceIfExists -ServiceName $mysqlServiceName
        }
    }

    Assert-PortAvailable -Port $mysqlPort -Name 'MySQL'
    Assert-PortAvailable -Port $backendPort -Name 'Backend'
    Assert-PortAvailable -Port $frontendPort -Name 'Frontend'
    Write-Ok "Required ports are available."

    if ($DryRun) {
        $dryDir = Join-Path $installerHome 'logs\dry-run'
        if (Test-Path -LiteralPath $dryDir) {
            Remove-Item -LiteralPath $dryDir -Recurse -Force
        }
        New-Directory -Path $dryDir
        Write-Step "Rendering templates for dry-run validation"
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
            JAVA_EXE = Convert-ToConfigPath (Join-Path $paths.Java 'bin\java.exe')
            JAVA_XMS = [string]$config['JavaXms']
            JAVA_XMX = [string]$config['JavaXmx']
            BACKEND_JAR = Convert-ToConfigPath (Join-Path $paths.Backend ([string]$config['BackendJarName']))
            CONF_DIR = Convert-ToConfigPath $paths.Conf
            BACKEND_DIR = Convert-ToConfigPath $paths.Backend
            BACKEND_SERVICE_LOG_DIR = Convert-ToConfigPath $paths.ServiceLog
            BACKEND_MONITOR_TASK_NAME = $backendMonitorTaskName
            BACKEND_MONITOR_INTERVAL_SECONDS = $backendMonitorIntervalSeconds
            BACKEND_MONITOR_SCRIPT = Convert-ToConfigPath (Join-Path $paths.Backend 'backend-prod-monitor.ps1')
            BACKEND_MONITOR_STOP_FILE = Convert-ToConfigPath (Join-Path $paths.Backend '.backend-prod-monitor.stop')
            BACKEND_MONITOR_LOG_FILE = Convert-ToConfigPath (Join-Path $paths.BackendLog 'backend-monitor.log')
            MYSQL_SERVICE_NAME = $mysqlServiceName
            BACKEND_SERVICE_NAME = $backendServiceName
            NGINX_SERVICE_NAME = $nginxServiceName
            NGINX_EXE = Convert-ToConfigPath (Join-Path $paths.Nginx 'nginx.exe')
            NGINX_HOME = Convert-ToConfigPath $paths.Nginx
            NGINX_SERVICE_LOG_DIR = Convert-ToConfigPath $paths.ServiceLog
        }
        Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\application-prod.yml.template') -DestinationPath (Join-Path $dryDir 'application-prod.yml') -Tokens $tokens
        Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\my.ini.template') -DestinationPath (Join-Path $dryDir 'my.ini') -Tokens $tokens
        Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\nginx.conf.template') -DestinationPath (Join-Path $dryDir 'nginx.conf') -Tokens $tokens
        Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\backend-service.xml.template') -DestinationPath (Join-Path $dryDir 'backend-service.xml') -Tokens $tokens
        Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\nginx-service.xml.template') -DestinationPath (Join-Path $dryDir 'nginx-service.xml') -Tokens $tokens
        Write-BackendManagementScripts -InstallerHome $installerHome -DestinationDir (Join-Path $dryDir 'backend') -Tokens $tokens
        Write-Ok "Dry-run completed. Rendered files: $dryDir"
        return
    }

    Write-Step "Creating install directories"
    foreach ($dir in $paths.Values) {
        New-Directory -Path ([string]$dir)
    }
    New-Directory -Path (Join-Path $paths.Logs 'mysql')

    Write-Step "Installing VC++ runtime"
    Invoke-Checked -FilePath $vcRedist -Arguments @('/install', '/quiet', '/norestart') -AcceptedExitCodes @(0, 3010) -ErrorMessage 'VC++ runtime installation failed.'
    Write-Ok "VC++ runtime installed or already present."

    Write-Step "Extracting runtime packages"
    Expand-ZipToDirectory -ZipPath $javaZip -Destination $paths.Java -InstallRoot $root
    Expand-ZipToDirectory -ZipPath $mysqlZip -Destination $paths.Mysql -InstallRoot $root
    Expand-ZipToDirectory -ZipPath $nginxZip -Destination $paths.Nginx -InstallRoot $root
    Write-Ok "Runtime packages extracted."

    $javaExe = Join-Path $paths.Java 'bin\java.exe'
    $mysqldExe = Join-Path $paths.Mysql 'bin\mysqld.exe'
    $mysqlExe = Join-Path $paths.Mysql 'bin\mysql.exe'
    $nginxExe = Join-Path $paths.Nginx 'nginx.exe'
    foreach ($file in @($javaExe, $mysqldExe, $mysqlExe, $nginxExe)) {
        if (-not (Test-Path -LiteralPath $file)) {
            Fail "Expected executable not found after extraction: $file"
        }
    }

    Write-Step "Copying application files"
    Copy-Item -LiteralPath $backendJarSource -Destination (Join-Path $paths.Backend ([string]$config['BackendJarName'])) -Force
    if (Test-Path -LiteralPath $paths.Frontend) {
        Remove-DirectorySafe -Root $root -Path $paths.Frontend
    }
    New-Directory -Path $paths.Frontend
    Copy-Item -Path (Join-Path $frontendSource '*') -Destination $paths.Frontend -Recurse -Force
    Write-Ok "Application files copied."

    Write-Step "Rendering configuration files"
    $backendJar = Join-Path $paths.Backend ([string]$config['BackendJarName'])
    $backendServiceExe = Join-Path $paths.Service "$backendServiceName.exe"
    $nginxServiceExe = Join-Path $paths.Service "$nginxServiceName.exe"
    Copy-Item -LiteralPath $winswExe -Destination $backendServiceExe -Force
    Copy-Item -LiteralPath $winswExe -Destination $nginxServiceExe -Force

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
        JAVA_EXE = Convert-ToConfigPath $javaExe
        JAVA_XMS = [string]$config['JavaXms']
        JAVA_XMX = [string]$config['JavaXmx']
        BACKEND_JAR = Convert-ToConfigPath $backendJar
        CONF_DIR = Convert-ToConfigPath $paths.Conf
        BACKEND_DIR = Convert-ToConfigPath $paths.Backend
        BACKEND_SERVICE_LOG_DIR = Convert-ToConfigPath $paths.ServiceLog
        BACKEND_MONITOR_TASK_NAME = $backendMonitorTaskName
        BACKEND_MONITOR_INTERVAL_SECONDS = $backendMonitorIntervalSeconds
        BACKEND_MONITOR_SCRIPT = Convert-ToConfigPath (Join-Path $paths.Backend 'backend-prod-monitor.ps1')
        BACKEND_MONITOR_STOP_FILE = Convert-ToConfigPath (Join-Path $paths.Backend '.backend-prod-monitor.stop')
        BACKEND_MONITOR_LOG_FILE = Convert-ToConfigPath (Join-Path $paths.BackendLog 'backend-monitor.log')
        MYSQL_SERVICE_NAME = $mysqlServiceName
        BACKEND_SERVICE_NAME = $backendServiceName
        NGINX_SERVICE_NAME = $nginxServiceName
        NGINX_EXE = Convert-ToConfigPath $nginxExe
        NGINX_HOME = Convert-ToConfigPath $paths.Nginx
        NGINX_SERVICE_LOG_DIR = Convert-ToConfigPath $paths.ServiceLog
    }
    $myIni = Join-Path $paths.Conf 'my.ini'
    Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\application-prod.yml.template') -DestinationPath (Join-Path $paths.Conf 'application-prod.yml') -Tokens $tokens
    Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\my.ini.template') -DestinationPath $myIni -Tokens $tokens
    Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\nginx.conf.template') -DestinationPath (Join-Path $paths.Nginx 'conf\nginx.conf') -Tokens $tokens
    Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\backend-service.xml.template') -DestinationPath (Join-Path $paths.Service "$backendServiceName.xml") -Tokens $tokens
    Write-RenderedTemplate -TemplatePath (Join-Path $installerHome 'config\nginx-service.xml.template') -DestinationPath (Join-Path $paths.Service "$nginxServiceName.xml") -Tokens $tokens
    Write-BackendManagementScripts -InstallerHome $installerHome -DestinationDir $paths.Backend -Tokens $tokens
    $marker = [ordered]@{
        product = 'BufferPad'
        installedAt = (Get-Date).ToString('s')
        installRoot = $root
        mysqlServiceName = $mysqlServiceName
        backendServiceName = $backendServiceName
        nginxServiceName = $nginxServiceName
        backendMonitorTaskName = $backendMonitorTaskName
        mysqlPort = $mysqlPort
        backendPort = $backendPort
        frontendPort = $frontendPort
    }
    ($marker | ConvertTo-Json -Depth 4) | Set-Content -LiteralPath (Join-Path $root '.bufferpad-install.json') -Encoding ASCII
    Write-Ok "Configuration files rendered."

    Write-Step "Configuring Java environment variables"
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $paths.Java, 'Machine')
    $machinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $javaBin = Join-Path $paths.Java 'bin'
    if ($machinePath -notlike "*$javaBin*") {
        [Environment]::SetEnvironmentVariable('Path', ($machinePath.TrimEnd(';') + ';' + $javaBin), 'Machine')
    }
    Write-Ok "JAVA_HOME configured."

    Write-Step "Initializing and starting MySQL"
    if ($ResetData -and (Test-Path -LiteralPath $paths.MysqlData)) {
        Remove-DirectorySafe -Root $root -Path $paths.MysqlData
    }
    New-Directory -Path $paths.MysqlData
    $mysqlDataFiles = @(Get-ChildItem -LiteralPath $paths.MysqlData -Force -ErrorAction SilentlyContinue)
    if ($mysqlDataFiles.Count -eq 0) {
        Invoke-ProcessChecked -FilePath $mysqldExe `
            -Arguments @("--defaults-file=$myIni", '--initialize-insecure', '--console') `
            -StdOutPath (Join-Path $paths.Logs 'mysql\initialize.out.log') `
            -StdErrPath (Join-Path $paths.Logs 'mysql\initialize.err.log') `
            -ErrorMessage 'MySQL data initialization failed.'
    } else {
        Write-Warn "MySQL data directory is not empty, skipping initialization: $($paths.MysqlData)"
    }
    Invoke-Checked -FilePath $mysqldExe -Arguments @('--install', $mysqlServiceName, "--defaults-file=$myIni") -ErrorMessage "Failed to install MySQL service $mysqlServiceName."
    Set-BufferPadServiceAutoStart -ServiceName $mysqlServiceName
    Start-Service -Name $mysqlServiceName

    $clientNoPassword = Join-Path $paths.Conf 'mysql-client-nopassword.cnf'
    $clientWithPassword = Join-Path $paths.Conf 'mysql-client.cnf'
    New-MySqlClientFile -Path $clientNoPassword -User 'root' -Password '' -Port $mysqlPort -NoPassword
    New-MySqlClientFile -Path $clientWithPassword -User 'root' -Password ([string]$config['MysqlRootPassword']) -Port $mysqlPort

    $readyWithNoPassword = $false
    $readyWithPassword = $false
    $deadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $deadline) {
        if (Test-MySqlConnection -MysqlExe $mysqlExe -ClientFile $clientWithPassword) {
            $readyWithPassword = $true
            break
        }
        if (Test-MySqlConnection -MysqlExe $mysqlExe -ClientFile $clientNoPassword) {
            $readyWithNoPassword = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not $readyWithNoPassword -and -not $readyWithPassword) {
        Fail "Cannot connect to MySQL as root. If this is an old installation with a different password, use -ResetData or update config\install.config.ps1."
    }

    $bootstrapClient = if ($readyWithPassword) { $clientWithPassword } else { $clientNoPassword }
    $dbName = [string]$config['DatabaseName']
    $rootPassword = [string]$config['MysqlRootPassword']
    $rootPasswordSql = $rootPassword.Replace("'", "''")
    $appUser = [string]$config['MysqlUser']
    $appPassword = [string]$config['MysqlPassword']
    $appPasswordSql = $appPassword.Replace("'", "''")
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
    Invoke-MySqlSql -MysqlExe $mysqlExe -ClientFile $bootstrapClient -Sql $bootstrapSql -ErrorMessage 'MySQL bootstrap SQL failed.'
    Wait-MySqlConnection -MysqlExe $mysqlExe -ClientFile $clientWithPassword -TimeoutSeconds 30
    Write-Ok "MySQL is ready."

    if (-not $SkipDbImport) {
        Write-Step "Importing database dump"
        $importCommand = '"' + $mysqlExe + '" --defaults-extra-file="' + $clientWithPassword + '" ' + $dbName + ' < "' + $dbDumpSource + '"'
        Invoke-CmdChecked -Command $importCommand -ErrorMessage "Database import failed from $dbDumpSource."
        Write-Ok "Database imported."
    } else {
        Write-Warn "Skipping database import by request."
    }

    $migrationFiles = @(Get-ChildItem -LiteralPath $dbMigrationDir -Filter '*.sql' -File | Sort-Object Name)
    foreach ($migrationFile in $migrationFiles) {
        Write-Step "Applying database migration $($migrationFile.Name)"
        $migrationCommand = '"' + $mysqlExe + '" --defaults-extra-file="' + $clientWithPassword + '" ' + $dbName + ' < "' + $migrationFile.FullName + '"'
        Invoke-CmdChecked -Command $migrationCommand -ErrorMessage "Database migration failed: $($migrationFile.FullName)"
        Write-Ok "Database migration applied: $($migrationFile.Name)"
    }

    Write-Step "Installing backend service"
    Invoke-Checked -FilePath $backendServiceExe -Arguments @('install') -ErrorMessage "Failed to install service $backendServiceName."
    Set-BufferPadServiceAutoStart -ServiceName $backendServiceName -Delayed
    Start-Service -Name $backendServiceName
    Write-Ok "Backend service started."

    Write-Step "Installing nginx service"
    Invoke-Checked -FilePath $nginxExe -Arguments @('-p', $paths.Nginx, '-c', 'conf/nginx.conf', '-t') -ErrorMessage 'nginx config test failed.'
    Invoke-Checked -FilePath $nginxServiceExe -Arguments @('install') -ErrorMessage "Failed to install service $nginxServiceName."
    Set-BufferPadServiceAutoStart -ServiceName $nginxServiceName -Delayed
    Start-Service -Name $nginxServiceName
    Write-Ok "nginx service started."

    Write-Step "Installing backend prod monitor task"
    Invoke-Checked `
        -FilePath (Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe') `
        -Arguments @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $paths.Backend 'backend-prod-monitor-start.ps1')) `
        -ErrorMessage "Failed to install backend prod monitor task $backendMonitorTaskName."
    Write-Ok "Backend prod monitor task installed and started."

    if (-not $NoFirewallRule) {
        Write-Step "Configuring Windows firewall"
        $ruleName = "BufferPad Frontend $frontendPort"
        $rule = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
        if ($null -eq $rule) {
            New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Protocol TCP -LocalPort $frontendPort -Action Allow | Out-Null
        }
        Write-Ok "Firewall rule is ready: $ruleName"
    }

    Write-Step "Validating services and HTTP endpoints"
    foreach ($serviceName in @($mysqlServiceName, $backendServiceName, $nginxServiceName)) {
        $service = Get-Service -Name $serviceName -ErrorAction Stop
        if ($service.Status -ne 'Running') {
            Fail "Service $serviceName is not running. Current status: $($service.Status)"
        }
        $escapedName = $serviceName.Replace("'", "''")
        $startup = Get-CimInstance -ClassName Win32_Service -Filter "Name='$escapedName'" -ErrorAction Stop
        if ($startup.StartMode -ne 'Auto') {
            Fail "Service $serviceName is running but is not configured for automatic startup. Current StartMode: $($startup.StartMode)"
        }
    }
    $monitorTask = Get-ScheduledTask -TaskName $backendMonitorTaskName -ErrorAction Stop
    if ($monitorTask.State -eq 'Disabled') {
        Fail "Backend prod monitor task is disabled: $backendMonitorTaskName"
    }
    Wait-HttpOk -Url "http://127.0.0.1:$backendPort/actuator/health" -TimeoutSeconds 90 | Out-Null
    Wait-HttpOk -Url "http://127.0.0.1:$frontendPort/" -TimeoutSeconds 30 | Out-Null
    Wait-HttpOk -Url "http://127.0.0.1:$frontendPort/api/opcConfig/page" -TimeoutSeconds 30 | Out-Null

    Write-Ok "BufferPad installed successfully."
    Write-Host ""
    Write-Host "Open: http://127.0.0.1:$frontendPort" -ForegroundColor Green
    Write-Host "LAN:  http://<this-machine-ip>:$frontendPort" -ForegroundColor Green
} catch {
    Write-Host ""
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Install log: $transcriptPath" -ForegroundColor Yellow
    exit 1
} finally {
    try {
        Stop-Transcript | Out-Null
    } catch {
    }
}
