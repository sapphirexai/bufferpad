param(
    [string]$InstallRoot,
    [string]$MysqlServiceName,
    [string]$BackendServiceName,
    [string]$NginxServiceName,
    [switch]$DryRun,
    [switch]$KeepData,
    [switch]$AllowMissingMarker
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$logDir = Join-Path $installerHome 'logs'
New-Directory -Path $logDir
$transcriptPath = Join-Path $logDir ("uninstall-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + ".log")

try {
    Start-Transcript -Path $transcriptPath -Force | Out-Null
} catch {
    Write-Warn "Could not start transcript: $($_.Exception.Message)"
}

function Invoke-UninstallAction {
    param(
        [string]$Message,
        [scriptblock]$Action
    )

    if ($DryRun) {
        Write-Warn "[DryRun] $Message"
        return
    }
    Write-Host $Message -ForegroundColor DarkGray
    & $Action
}

function Get-ServicePathName {
    param([string]$ServiceName)
    $service = Get-CimInstance Win32_Service -Filter "Name='$ServiceName'" -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        return $null
    }
    return [string]$service.PathName
}

function Assert-ServiceOwnedByInstall {
    param(
        [string]$ServiceName,
        [string]$Root
    )

    $pathName = Get-ServicePathName -ServiceName $ServiceName
    if ([string]::IsNullOrWhiteSpace($pathName)) {
        Fail "Service $ServiceName exists but its binary path cannot be determined. Refusing to remove it."
    }

    $normalizedRoot = [IO.Path]::GetFullPath($Root).TrimEnd('\')
    if ($pathName.IndexOf($normalizedRoot, [StringComparison]::OrdinalIgnoreCase) -lt 0) {
        Fail "Service $ServiceName exists but its binary path is outside install root. Path=$pathName Root=$normalizedRoot. Refusing to remove it."
    }
}

function Remove-BufferPadWinSWService {
    param(
        [string]$ServiceName,
        [string]$ExePath,
        [string]$Root
    )

    $service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        Write-Ok "$ServiceName service is not installed."
        return
    }

    Assert-ServiceOwnedByInstall -ServiceName $ServiceName -Root $Root
    Invoke-UninstallAction "Stopping and uninstalling $ServiceName" {
        Stop-ServiceIfExists -Name $ServiceName
        if (Test-Path -LiteralPath $ExePath) {
            Invoke-Checked -FilePath $ExePath -Arguments @('uninstall') -ErrorMessage "Failed to uninstall service $ServiceName."
        } else {
            sc.exe delete $ServiceName | Out-Null
        }
        Start-Sleep -Seconds 2
    }
}

function Remove-BufferPadNativeService {
    param(
        [string]$ServiceName,
        [string]$Root
    )

    $service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        Write-Ok "$ServiceName service is not installed."
        return
    }

    Assert-ServiceOwnedByInstall -ServiceName $ServiceName -Root $Root
    Invoke-UninstallAction "Stopping and deleting $ServiceName" {
        Stop-ServiceIfExists -Name $ServiceName
        sc.exe delete $ServiceName | Out-Null
        Start-Sleep -Seconds 2
    }
}

function Stop-InstallRootProcesses {
    param([string]$Root)

    $rootFull = [IO.Path]::GetFullPath($Root).TrimEnd('\')
    $processes = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $name = [string]$_.Name
            $tracked = $name -in @('java.exe', 'javaw.exe', 'mysqld.exe', 'nginx.exe', 'BufferPadBackend.exe', 'BufferPadNginx.exe')
            $exe = [string]$_.ExecutablePath
            $cmd = [string]$_.CommandLine
            $tracked -and (
                (-not [string]::IsNullOrWhiteSpace($exe) -and $exe.IndexOf($rootFull, [StringComparison]::OrdinalIgnoreCase) -ge 0) -or
                (-not [string]::IsNullOrWhiteSpace($cmd) -and $cmd.IndexOf($rootFull, [StringComparison]::OrdinalIgnoreCase) -ge 0)
            )
        })

    foreach ($proc in $processes) {
        Invoke-UninstallAction "Stopping leftover process PID=$($proc.ProcessId) Name=$($proc.Name)" {
            Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
        }
    }

    if ($processes.Count -eq 0) {
        Write-Ok "No leftover BufferPad runtime process found."
    }
}

function Remove-BufferPadFirewallRule {
    param([int]$FrontendPort)

    $ruleName = "BufferPad Frontend $FrontendPort"
    $rule = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
    if ($null -eq $rule) {
        Write-Ok "Firewall rule is not present: $ruleName"
        return
    }
    Invoke-UninstallAction "Removing firewall rule $ruleName" {
        Remove-NetFirewallRule -DisplayName $ruleName
    }
}

function Remove-BufferPadJavaEnvironment {
    param([string]$Root)

    $rootFull = [IO.Path]::GetFullPath($Root).TrimEnd('\')
    foreach ($target in @('Machine', 'User')) {
        $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', $target)
        if (-not [string]::IsNullOrWhiteSpace($javaHome) -and $javaHome.IndexOf($rootFull, [StringComparison]::OrdinalIgnoreCase) -eq 0) {
            Invoke-UninstallAction "Removing $target JAVA_HOME because it points to BufferPad: $javaHome" {
                [Environment]::SetEnvironmentVariable('JAVA_HOME', $null, $target)
            }
        } else {
            Write-Ok "$target JAVA_HOME does not point to BufferPad; leaving it unchanged."
        }

        $envPath = [Environment]::GetEnvironmentVariable('Path', $target)
        if ([string]::IsNullOrWhiteSpace($envPath)) {
            continue
        }

        $parts = @($envPath -split ';' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        $kept = New-Object System.Collections.Generic.List[string]
        $removed = New-Object System.Collections.Generic.List[string]
        foreach ($part in $parts) {
            $trimmed = $part.Trim().Trim('"')
            if ($trimmed.IndexOf($rootFull, [StringComparison]::OrdinalIgnoreCase) -eq 0) {
                $removed.Add($trimmed)
            } else {
                $kept.Add($trimmed)
            }
        }

        if ($removed.Count -eq 0) {
            Write-Ok "$target Path has no BufferPad entries."
            continue
        }

        Invoke-UninstallAction "Removing BufferPad entries from $target Path: $($removed -join ', ')" {
            [Environment]::SetEnvironmentVariable('Path', ($kept -join ';'), $target)
        }
    }
}

function Remove-InstallRoot {
    param(
        [string]$Root,
        [switch]$KeepData
    )

    if (-not (Test-Path -LiteralPath $Root)) {
        Write-Ok "Install root is already absent: $Root"
        return
    }

    if ($KeepData) {
        $preservedData = Join-Path $Root 'data'
        Write-Warn "Keeping data directory by request: $preservedData"
        $children = @(Get-ChildItem -LiteralPath $Root -Force | Where-Object { $_.Name -notin @('data', '.bufferpad-install.json') })
        foreach ($child in $children) {
            $target = $child.FullName
            Invoke-UninstallAction "Removing $target" {
                Remove-Item -LiteralPath $target -Recurse -Force
            }
        }
        return
    }

    Invoke-UninstallAction "Removing install root $Root" {
        Remove-DirectorySafe -Root (Split-Path -Parent $Root) -Path $Root
    }
}

function Assert-UninstallResult {
    param(
        [hashtable]$Config,
        [string]$Root,
        [switch]$KeepData
    )

    foreach ($serviceName in @([string]$Config['NginxServiceName'], [string]$Config['BackendServiceName'], [string]$Config['MysqlServiceName'])) {
        $service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
        if ($null -ne $service) {
            Fail "Verification failed: service still exists: $serviceName"
        }
    }

    if ($Config.ContainsKey('BackendMonitorTaskName')) {
        $taskName = [string]$Config['BackendMonitorTaskName']
        if (-not [string]::IsNullOrWhiteSpace($taskName) -and $null -ne (Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue)) {
            Fail "Verification failed: backend monitor task still exists: $taskName"
        }
    }

    $ruleName = "BufferPad Frontend $([int]$Config['FrontendPort'])"
    if ($null -ne (Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue)) {
        Fail "Verification failed: firewall rule still exists: $ruleName"
    }

    $rootExists = Test-Path -LiteralPath $Root
    if (-not $KeepData -and $rootExists) {
        Fail "Verification failed: install root still exists: $Root"
    }
    if ($KeepData -and -not $rootExists) {
        Fail "Verification failed: install root was removed even though -KeepData was used."
    }

    $rootFull = [IO.Path]::GetFullPath($Root).TrimEnd('\')
    if (Test-Path -LiteralPath (Join-Path $Root 'java')) {
        Fail "Verification failed: BufferPad Java directory still exists: $(Join-Path $Root 'java')"
    }

    foreach ($target in @('Machine', 'User')) {
        $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', $target)
        if (-not [string]::IsNullOrWhiteSpace($javaHome) -and $javaHome.IndexOf($rootFull, [StringComparison]::OrdinalIgnoreCase) -eq 0) {
            Fail "Verification failed: $target JAVA_HOME still points to BufferPad: $javaHome"
        }

        $envPath = [Environment]::GetEnvironmentVariable('Path', $target)
        if (-not [string]::IsNullOrWhiteSpace($envPath)) {
            $bufferPadPathEntries = @($envPath -split ';' | Where-Object {
                $entry = $_.Trim().Trim('"')
                -not [string]::IsNullOrWhiteSpace($entry) -and $entry.IndexOf($rootFull, [StringComparison]::OrdinalIgnoreCase) -eq 0
            })
            if ($bufferPadPathEntries.Count -gt 0) {
                Fail "Verification failed: $target Path still contains BufferPad entries: $($bufferPadPathEntries -join ', ')"
            }
        }
    }

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $javaPaths = @(cmd.exe /c 'where java 2>nul')
        $whereJavaExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($whereJavaExitCode -eq 0 -and $javaPaths.Count -gt 0) {
        $externalJavaPaths = @($javaPaths | Where-Object { ([string]$_).IndexOf($rootFull, [StringComparison]::OrdinalIgnoreCase) -lt 0 })
        if ($externalJavaPaths.Count -gt 0) {
            Write-Warn "External Java is still available and was left untouched: $($externalJavaPaths -join ', ')"
        }
    }

    Write-Ok "Uninstall verification passed."
}

try {
    $config = Read-InstallConfig -InstallerHome $installerHome -InstallRootOverride $InstallRoot
    if (-not [string]::IsNullOrWhiteSpace($MysqlServiceName)) { $config['MysqlServiceName'] = $MysqlServiceName }
    if (-not [string]::IsNullOrWhiteSpace($BackendServiceName)) { $config['BackendServiceName'] = $BackendServiceName }
    if (-not [string]::IsNullOrWhiteSpace($NginxServiceName)) { $config['NginxServiceName'] = $NginxServiceName }
    $root = [string]$config['InstallRoot']
    $serviceDir = Join-Path $root 'service'

    Write-Step "BufferPad one-click uninstall"
    Write-Host "Install root: $root"
    Write-Host "DryRun: $DryRun"
    Write-Host "KeepData: $KeepData"

    if (-not $DryRun) {
        Assert-Admin
    }

    Assert-InstallRootSafeForDelete -Root $root -AllowMissingMarker:$AllowMissingMarker

    Write-Step "Removing backend prod monitor"
    Stop-BufferPadBackendMonitor `
        -TaskName ([string]$config['BackendMonitorTaskName']) `
        -MonitorScript (Join-Path $root 'backend\backend-prod-monitor.ps1') `
        -StopFile (Join-Path $root 'backend\.backend-prod-monitor.stop') `
        -Root $root `
        -DryRun:$DryRun

    Write-Step "Removing services"
    Remove-BufferPadWinSWService -ServiceName ([string]$config['NginxServiceName']) -ExePath (Join-Path $serviceDir "$([string]$config['NginxServiceName']).exe") -Root $root
    Remove-BufferPadWinSWService -ServiceName ([string]$config['BackendServiceName']) -ExePath (Join-Path $serviceDir "$([string]$config['BackendServiceName']).exe") -Root $root
    Remove-BufferPadNativeService -ServiceName ([string]$config['MysqlServiceName']) -Root $root

    Write-Step "Stopping leftover BufferPad processes"
    Stop-InstallRootProcesses -Root $root

    Write-Step "Removing firewall and environment changes"
    Remove-BufferPadFirewallRule -FrontendPort ([int]$config['FrontendPort'])
    Remove-BufferPadJavaEnvironment -Root $root

    Write-Step "Removing BufferPad files"
    Remove-InstallRoot -Root $root -KeepData:$KeepData

    if ($DryRun) {
        Write-Ok "Dry-run completed. No changes were made."
    } else {
        Write-Step "Verifying uninstall result"
        Assert-UninstallResult -Config $config -Root $root -KeepData:$KeepData
        Write-Ok "BufferPad was uninstalled successfully."
    }
} catch {
    Write-Host ""
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Uninstall log: $transcriptPath" -ForegroundColor Yellow
    exit 1
} finally {
    try {
        Stop-Transcript | Out-Null
    } catch {
    }
}
