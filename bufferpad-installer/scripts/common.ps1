Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-InstallerHome {
    return (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
}

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Fail {
    param([string]$Message)
    throw "ERROR: $Message"
}

function Test-IsAdmin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Assert-Admin {
    if (-not (Test-IsAdmin)) {
        Fail "Please run PowerShell as Administrator, then run this script again."
    }
}

function New-Directory {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }
}

function Convert-ToConfigPath {
    param([string]$Path)
    return ([IO.Path]::GetFullPath($Path)).Replace('\', '/')
}

function Assert-InsideRoot {
    param(
        [string]$Root,
        [string]$Path
    )
    $rootFull = [IO.Path]::GetFullPath($Root).TrimEnd('\') + '\'
    $pathFull = [IO.Path]::GetFullPath($Path)
    if (-not $pathFull.StartsWith($rootFull, [StringComparison]::OrdinalIgnoreCase)) {
        Fail "Refusing to modify path outside install root. Root=$rootFull Path=$pathFull"
    }
}

function Remove-DirectorySafe {
    param(
        [string]$Root,
        [string]$Path
    )
    Assert-InsideRoot -Root $Root -Path $Path
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
}

function Test-PathInsideRoot {
    param(
        [string]$Root,
        [string]$Path
    )
    if ([string]::IsNullOrWhiteSpace($Root) -or [string]::IsNullOrWhiteSpace($Path)) {
        return $false
    }
    $rootFull = [IO.Path]::GetFullPath($Root).TrimEnd('\') + '\'
    $pathFull = [IO.Path]::GetFullPath($Path).TrimEnd('\') + '\'
    return $pathFull.StartsWith($rootFull, [StringComparison]::OrdinalIgnoreCase)
}

function Assert-InstallRootSafeForDelete {
    param(
        [string]$Root,
        [switch]$AllowMissingMarker
    )

    $rootFull = [IO.Path]::GetFullPath($Root).TrimEnd('\')
    $systemDrive = [Environment]::GetEnvironmentVariable('SystemDrive')
    $invalidRoots = @(
        [IO.Path]::GetPathRoot($rootFull).TrimEnd('\'),
        $systemDrive,
        [Environment]::GetFolderPath('Windows'),
        [Environment]::GetFolderPath('ProgramFiles'),
        [Environment]::GetFolderPath('ProgramFilesX86'),
        [Environment]::GetFolderPath('UserProfile')
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object { [IO.Path]::GetFullPath($_).TrimEnd('\') }

    foreach ($invalid in $invalidRoots) {
        if ($rootFull.Equals($invalid, [StringComparison]::OrdinalIgnoreCase)) {
            Fail "Refusing to delete unsafe install root: $rootFull"
        }
    }

    $marker = Join-Path $rootFull '.bufferpad-install.json'
    if ((Test-Path -LiteralPath $rootFull) -and -not (Test-Path -LiteralPath $marker) -and -not $AllowMissingMarker) {
        Fail "Install marker is missing: $marker. Refusing to remove the directory. If this is an old BufferPad install, rerun with -AllowMissingMarker."
    }
}

function Read-InstallConfig {
    param(
        [string]$InstallerHome,
        [string]$InstallRootOverride
    )

    $configPath = Join-Path $InstallerHome 'config\install.config.ps1'
    if (-not (Test-Path -LiteralPath $configPath)) {
        Fail "Missing config file: $configPath"
    }

    $config = & $configPath
    if ($null -eq $config -or -not ($config -is [hashtable])) {
        Fail "Config file must return a hashtable: $configPath"
    }

    $defaults = @{
        InstallRoot = 'C:\bufferpad'
        AdminUsername = 'admin'
        InitialAdminPassword = 'example-admin-password'
        FrontendPort = 18088
        BackendPort = 9001
        MysqlPort = 3306
        DatabaseName = 'wms_opc'
        MysqlRootPassword = 'CHANGE_ME_DB_PASSWORD'
        MysqlUser = 'root'
        MysqlPassword = 'CHANGE_ME_DB_PASSWORD'
        MysqlServiceName = 'BufferPadMySQL'
        BackendServiceName = 'BufferPadBackend'
        NginxServiceName = 'BufferPadNginx'
        BackendMonitorTaskName = 'BufferPadBackendMonitor'
        BackendMonitorIntervalSeconds = 15
        JavaPackagePattern = 'jdk-17-windows-x64.zip'
        MysqlPackagePattern = 'mysql-8.0.*-winx64.zip'
        NginxPackagePattern = 'nginx-windows.zip'
        WinSWPackagePattern = 'winsw-x64.exe'
        VCRedistPattern = 'VC_redist.x64.exe'
        BackendJarName = 'opc.jar'
        DbDumpFileName = 'wms_opc.sql'
        JavaXms = '256m'
        JavaXmx = '512m'
    }

    foreach ($key in $defaults.Keys) {
        if (-not $config.ContainsKey($key) -or $null -eq $config[$key] -or [string]::IsNullOrWhiteSpace([string]$config[$key])) {
            $config[$key] = $defaults[$key]
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($InstallRootOverride)) {
        $config['InstallRoot'] = $InstallRootOverride
    }

    $config['InstallRoot'] = [IO.Path]::GetFullPath([string]$config['InstallRoot']).TrimEnd('\')
    return $config
}

function Find-Package {
    param(
        [string]$InstallerHome,
        [string]$Pattern,
        [string]$DisplayName
    )
    $packageDir = Join-Path $InstallerHome 'packages'
    $matches = @(Get-ChildItem -Path $packageDir -Filter $Pattern -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)
    if ($matches.Count -eq 0) {
        Fail "Missing package for $DisplayName. Expected '$Pattern' under $packageDir. Run scripts\download-packages.ps1 or put the file there manually."
    }
    return $matches[0].FullName
}

function Invoke-Checked {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$ErrorMessage,
        [int[]]$AcceptedExitCodes = @(0)
    )

    Write-Host "Running: $FilePath $($Arguments -join ' ')" -ForegroundColor DarkGray
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $FilePath @Arguments
        $code = $LASTEXITCODE
        if ($AcceptedExitCodes -notcontains $code) {
            Fail "$ErrorMessage ExitCode=$code"
        }
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Invoke-ProcessChecked {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$ErrorMessage,
        [string]$StdOutPath,
        [string]$StdErrPath,
        [int[]]$AcceptedExitCodes = @(0)
    )

    New-Directory -Path (Split-Path -Parent $StdOutPath)
    New-Directory -Path (Split-Path -Parent $StdErrPath)
    Write-Host "Running: $FilePath $($Arguments -join ' ')" -ForegroundColor DarkGray
    $process = Start-Process -FilePath $FilePath `
        -ArgumentList $Arguments `
        -Wait `
        -PassThru `
        -WindowStyle Hidden `
        -RedirectStandardOutput $StdOutPath `
        -RedirectStandardError $StdErrPath
    if ($AcceptedExitCodes -notcontains $process.ExitCode) {
        $stderr = ''
        if (Test-Path -LiteralPath $StdErrPath) {
            $stderr = (Get-Content -LiteralPath $StdErrPath -Raw -ErrorAction SilentlyContinue)
        }
        Fail "$ErrorMessage ExitCode=$($process.ExitCode) Error=$stderr"
    }
}

function Invoke-CmdChecked {
    param(
        [string]$Command,
        [string]$ErrorMessage
    )
    Write-Host "Running: cmd.exe /c $Command" -ForegroundColor DarkGray
    cmd.exe /c $Command
    $code = $LASTEXITCODE
    if ($code -ne 0) {
        Fail "$ErrorMessage ExitCode=$code"
    }
}

function Write-RenderedTemplate {
    param(
        [string]$TemplatePath,
        [string]$DestinationPath,
        [hashtable]$Tokens
    )
    if (-not (Test-Path -LiteralPath $TemplatePath)) {
        Fail "Missing template: $TemplatePath"
    }
    $text = Get-Content -LiteralPath $TemplatePath -Raw
    foreach ($key in $Tokens.Keys) {
        $text = $text.Replace('{{' + $key + '}}', [string]$Tokens[$key])
    }
    $unresolved = [regex]::Matches($text, '\{\{[A-Z0-9_]+\}\}')
    if ($unresolved.Count -gt 0) {
        $names = ($unresolved | ForEach-Object { $_.Value } | Select-Object -Unique) -join ', '
        Fail "Template has unresolved token(s): $names in $TemplatePath"
    }
    New-Directory -Path (Split-Path -Parent $DestinationPath)
    Set-Content -LiteralPath $DestinationPath -Value $text -Encoding ASCII
}

function Expand-ZipToDirectory {
    param(
        [string]$ZipPath,
        [string]$Destination,
        [string]$InstallRoot
    )
    if (-not (Test-Path -LiteralPath $ZipPath)) {
        Fail "Zip package not found: $ZipPath"
    }

    Remove-DirectorySafe -Root $InstallRoot -Path $Destination
    New-Directory -Path $Destination

    $tempDir = Join-Path $InstallRoot ('_extract_' + [Guid]::NewGuid().ToString('N'))
    New-Directory -Path $tempDir
    try {
        Expand-Archive -LiteralPath $ZipPath -DestinationPath $tempDir -Force
        $dirs = @(Get-ChildItem -LiteralPath $tempDir -Directory)
        $files = @(Get-ChildItem -LiteralPath $tempDir -File)
        if ($dirs.Count -eq 1 -and $files.Count -eq 0) {
            Copy-Item -Path (Join-Path $dirs[0].FullName '*') -Destination $Destination -Recurse -Force
        } else {
            Copy-Item -Path (Join-Path $tempDir '*') -Destination $Destination -Recurse -Force
        }
    } finally {
        Remove-DirectorySafe -Root $InstallRoot -Path $tempDir
    }
}

function Get-ListeningPortOwner {
    param([int]$Port)
    $pattern = '^\s*TCP\s+\S+:' + [regex]::Escape([string]$Port) + '\s+\S+\s+LISTENING\s+(\d+)\s*$'
    $lines = @(netstat -ano -p tcp | Select-String -Pattern $pattern)
    if ($lines.Count -eq 0) {
        return $null
    }
    $pidValue = [int]$lines[0].Matches[0].Groups[1].Value
    $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    return [pscustomobject]@{
        Port = $Port
        Pid = $pidValue
        ProcessName = if ($process) { $process.ProcessName } else { '<unknown>' }
    }
}

function Assert-PortAvailable {
    param(
        [int]$Port,
        [string]$Name
    )
    $owner = Get-ListeningPortOwner -Port $Port
    if ($null -ne $owner) {
        Fail "$Name port $Port is already in use by PID $($owner.Pid) ($($owner.ProcessName)). Stop that process or change config\install.config.ps1."
    }
}

function Get-FreeTcpPort {
    param(
        [int]$StartPort,
        [int]$MaxAttempts = 1000
    )
    for ($port = $StartPort; $port -lt ($StartPort + $MaxAttempts); $port++) {
        if ($null -eq (Get-ListeningPortOwner -Port $port)) {
            return $port
        }
    }
    Fail "Could not find a free TCP port starting at $StartPort."
}

function Assert-SimpleSqlIdentifier {
    param(
        [string]$Value,
        [string]$Name
    )
    if ($Value -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
        Fail "$Name must contain only letters, numbers, and underscores, and must not start with a number. Current value: $Value"
    }
}

function Stop-ServiceIfExists {
    param([string]$Name)
    $service = Get-Service -Name $Name -ErrorAction SilentlyContinue
    if ($null -ne $service -and $service.Status -ne 'Stopped') {
        Stop-Service -Name $Name -Force -ErrorAction Stop
        $service.WaitForStatus('Stopped', [TimeSpan]::FromSeconds(30))
    }
}

function Stop-BufferPadBackendMonitor {
    param(
        [string]$TaskName,
        [string]$MonitorScript,
        [string]$StopFile,
        [string]$Root,
        [switch]$DryRun
    )

    if ([string]::IsNullOrWhiteSpace($TaskName)) {
        return
    }

    $rootFull = [IO.Path]::GetFullPath($Root).TrimEnd('\')
    $monitorScriptFull = ''
    if (-not [string]::IsNullOrWhiteSpace($MonitorScript)) {
        $monitorScriptFull = [IO.Path]::GetFullPath($MonitorScript)
    }

    if ($DryRun) {
        Write-Warn "[DryRun] Would stop and remove backend monitor task $TaskName."
        return
    }

    if (-not [string]::IsNullOrWhiteSpace($StopFile)) {
        New-Directory -Path (Split-Path -Parent $StopFile)
        Set-Content -LiteralPath $StopFile -Value (Get-Date).ToString('s') -Encoding ASCII
    }

    $task = $null
    try {
        $task = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    } catch {
        $task = $null
    }
    if ($null -ne $task) {
        try {
            Stop-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
        } catch {
        }
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue
        Write-Ok "Backend monitor task removed: $TaskName"
    } else {
        Write-Ok "Backend monitor task is not registered: $TaskName"
    }

    Start-Sleep -Seconds 1
    $processes = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $name = [string]$_.Name
            $cmd = [string]$_.CommandLine
            $isPowerShell = $name -in @('powershell.exe', 'pwsh.exe')
            if (-not $isPowerShell -or [string]::IsNullOrWhiteSpace($cmd)) {
                $false
            } else {
                $cmd.IndexOf('backend-prod-monitor.ps1', [StringComparison]::OrdinalIgnoreCase) -ge 0 -and (
                    ([string]::IsNullOrWhiteSpace($monitorScriptFull) -or $cmd.IndexOf($monitorScriptFull, [StringComparison]::OrdinalIgnoreCase) -ge 0) -or
                    $cmd.IndexOf($rootFull, [StringComparison]::OrdinalIgnoreCase) -ge 0
                )
            }
        })
    foreach ($process in $processes) {
        try {
            Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
            Write-Ok "Stopped backend monitor process PID $($process.ProcessId)."
        } catch {
            Write-Warn "Could not stop backend monitor process PID $($process.ProcessId): $($_.Exception.Message)"
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($StopFile) -and (Test-Path -LiteralPath $StopFile)) {
        Remove-Item -LiteralPath $StopFile -Force -ErrorAction SilentlyContinue
    }
}

function Remove-WinSWServiceIfExists {
    param(
        [string]$ServiceName,
        [string]$ExePath
    )
    $service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        return
    }
    Stop-ServiceIfExists -Name $ServiceName
    if (Test-Path -LiteralPath $ExePath) {
        Invoke-Checked -FilePath $ExePath -Arguments @('uninstall') -ErrorMessage "Failed to uninstall service $ServiceName."
    } else {
        sc.exe delete $ServiceName | Out-Null
    }
}

function Remove-NativeServiceIfExists {
    param([string]$ServiceName)
    $service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        return
    }
    Stop-ServiceIfExists -Name $ServiceName
    sc.exe delete $ServiceName | Out-Null
    Start-Sleep -Seconds 2
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 60,
        [int[]]$AcceptedStatusCodes = @(200)
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastError = $null
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
            if ($AcceptedStatusCodes -contains [int]$response.StatusCode) {
                return $response
            }
            $lastError = "HTTP $($response.StatusCode)"
        } catch {
            $lastError = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }
    Fail "Timed out waiting for $Url. Last error: $lastError"
}

function Test-ZipFile {
    param([string]$Path)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = $null
    try {
        $zip = [IO.Compression.ZipFile]::OpenRead($Path)
        if ($zip.Entries.Count -eq 0) {
            Fail "Zip file is empty: $Path"
        }
    } finally {
        if ($null -ne $zip) {
            $zip.Dispose()
        }
    }
}
