param(
    [string]$InstallRoot
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$config = Read-InstallConfig -InstallerHome $installerHome -InstallRootOverride $InstallRoot
$root = [string]$config['InstallRoot']

function Get-ServiceStartupSummary {
    param([string]$ServiceName)

    $escapedName = $ServiceName.Replace("'", "''")
    $service = Get-CimInstance -ClassName Win32_Service -Filter "Name='$escapedName'" -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        return 'Startup=Unknown'
    }

    $registryPath = "HKLM:\SYSTEM\CurrentControlSet\Services\$ServiceName"
    $delayed = $false
    if (Test-Path -LiteralPath $registryPath) {
        $property = Get-ItemProperty -LiteralPath $registryPath -Name DelayedAutoStart -ErrorAction SilentlyContinue
        $delayed = ($null -ne $property -and [int]$property.DelayedAutoStart -eq 1)
    }

    if ($service.StartMode -eq 'Auto' -and $delayed) {
        return 'Startup=Automatic(Delayed)'
    }
    if ($service.StartMode -eq 'Auto') {
        return 'Startup=Automatic'
    }
    return "Startup=$($service.StartMode)"
}

Write-Step "Service status"
foreach ($serviceName in @([string]$config['MysqlServiceName'], [string]$config['BackendServiceName'], [string]$config['NginxServiceName'])) {
    $service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        Write-Warn "${serviceName}: not installed"
    } else {
        $startup = Get-ServiceStartupSummary -ServiceName $serviceName
        Write-Host "${serviceName}: $($service.Status), $startup"
    }
}

Write-Step "Backend monitor status"
$monitorTaskName = [string]$config['BackendMonitorTaskName']
try {
    $monitorTask = Get-ScheduledTask -TaskName $monitorTaskName -ErrorAction Stop
    $monitorInfo = Get-ScheduledTaskInfo -TaskName $monitorTaskName -ErrorAction SilentlyContinue
    if ($null -eq $monitorInfo) {
        Write-Host "${monitorTaskName}: $($monitorTask.State)"
    } else {
        Write-Host "${monitorTaskName}: $($monitorTask.State), LastRun=$($monitorInfo.LastRunTime), LastResult=$($monitorInfo.LastTaskResult)"
    }
} catch {
    $message = $_.Exception.Message
    if ($message -like '*Access is denied*' -or $message -like '*拒绝访问*') {
        Write-Warn "${monitorTaskName}: access denied. Run status.ps1 as Administrator to inspect the startup monitor task."
    } else {
        $previousErrorActionPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $schtasksOutput = @(cmd.exe /c "schtasks.exe /Query /TN `"$monitorTaskName`" /FO LIST 2>&1")
            $schtasksExitCode = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        if ($schtasksExitCode -ne 0 -and (($schtasksOutput -join "`n") -like '*Access is denied*' -or ($schtasksOutput -join "`n") -like '*拒绝访问*')) {
            Write-Warn "${monitorTaskName}: access denied. Run status.ps1 as Administrator to inspect the startup monitor task."
        } else {
            Write-Warn "${monitorTaskName}: not registered"
        }
    }
}

Write-Step "Port status"
foreach ($pair in @(
    @('MySQL', [int]$config['MysqlPort']),
    @('Backend', [int]$config['BackendPort']),
    @('Frontend', [int]$config['FrontendPort'])
)) {
    $owner = Get-ListeningPortOwner -Port $pair[1]
    if ($null -eq $owner) {
        Write-Warn "$($pair[0]) port $($pair[1]): not listening"
    } else {
        Write-Host "$($pair[0]) port $($pair[1]): PID $($owner.Pid) $($owner.ProcessName)"
    }
}

Write-Step "HTTP status"
foreach ($url in @(
    "http://127.0.0.1:$([int]$config['BackendPort'])/actuator/health",
    "http://127.0.0.1:$([int]$config['FrontendPort'])/",
    "http://127.0.0.1:$([int]$config['FrontendPort'])/api/opcConfig/page"
)) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 5
        Write-Host "$url -> HTTP $($response.StatusCode)"
    } catch {
        Write-Warn "$url -> $($_.Exception.Message)"
    }
}

Write-Step "Log files"
foreach ($dir in @(
    (Join-Path $root 'logs\backend'),
    (Join-Path $root 'logs\nginx'),
    (Join-Path $root 'logs\service'),
    (Join-Path $root 'logs\mysql')
)) {
    if (Test-Path -LiteralPath $dir) {
        Get-ChildItem -LiteralPath $dir -File -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 5 FullName, Length, LastWriteTime |
            Format-Table -AutoSize
    } else {
        Write-Warn "Log directory not found: $dir"
    }
}
