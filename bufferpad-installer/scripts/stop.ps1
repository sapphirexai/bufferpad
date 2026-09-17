param(
    [string]$InstallRoot
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$config = Read-InstallConfig -InstallerHome $installerHome -InstallRootOverride $InstallRoot

foreach ($serviceName in @([string]$config['NginxServiceName'], [string]$config['BackendServiceName'], [string]$config['MysqlServiceName'])) {
    $service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        Write-Warn "Service not found: $serviceName"
        continue
    }
    if ($service.Status -ne 'Stopped') {
        Write-Step "Stopping $serviceName"
        Stop-Service -Name $serviceName -Force
        $service.WaitForStatus('Stopped', [TimeSpan]::FromSeconds(60))
    }
    Write-Ok "$serviceName is stopped."
}
