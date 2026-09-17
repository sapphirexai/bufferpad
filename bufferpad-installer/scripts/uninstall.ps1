param(
    [string]$InstallRoot,
    [switch]$RemoveData
)

. (Join-Path $PSScriptRoot 'common.ps1')

Assert-Admin

if ($RemoveData) {
    Write-Warn "scripts\uninstall.ps1 -RemoveData delegates to scripts\uninstall-oneclick.ps1 for a full cleanup."
    $fullUninstallArgs = @()
    if (-not [string]::IsNullOrWhiteSpace($InstallRoot)) {
        $fullUninstallArgs += @('-InstallRoot', $InstallRoot)
    }
    & (Join-Path $PSScriptRoot 'uninstall-oneclick.ps1') @fullUninstallArgs
    exit $LASTEXITCODE
}

$installerHome = Get-InstallerHome
$config = Read-InstallConfig -InstallerHome $installerHome -InstallRootOverride $InstallRoot
$root = [string]$config['InstallRoot']
$serviceDir = Join-Path $root 'service'

Write-Step "Removing backend prod monitor"
Stop-BufferPadBackendMonitor `
    -TaskName ([string]$config['BackendMonitorTaskName']) `
    -MonitorScript (Join-Path $root 'backend\backend-prod-monitor.ps1') `
    -StopFile (Join-Path $root 'backend\.backend-prod-monitor.stop') `
    -Root $root

Write-Step "Removing BufferPad services"
Remove-WinSWServiceIfExists -ServiceName ([string]$config['NginxServiceName']) -ExePath (Join-Path $serviceDir "$([string]$config['NginxServiceName']).exe")
Remove-WinSWServiceIfExists -ServiceName ([string]$config['BackendServiceName']) -ExePath (Join-Path $serviceDir "$([string]$config['BackendServiceName']).exe")
Remove-NativeServiceIfExists -ServiceName ([string]$config['MysqlServiceName'])
Write-Ok "Services removed."

Write-Warn "Install files and database data are preserved at $root"
Write-Warn "Run scripts\uninstall.ps1 -RemoveData only when you really want a full cleanup."
