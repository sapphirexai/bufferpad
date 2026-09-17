param(
    [string]$InstallRoot
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$config = Read-InstallConfig -InstallerHome $installerHome -InstallRootOverride $InstallRoot

foreach ($serviceName in @([string]$config['MysqlServiceName'], [string]$config['BackendServiceName'], [string]$config['NginxServiceName'])) {
    $service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        Fail "Service not found: $serviceName. Run scripts\install.ps1 first."
    }
    if ($service.Status -ne 'Running') {
        Write-Step "Starting $serviceName"
        Start-Service -Name $serviceName
        $service.WaitForStatus('Running', [TimeSpan]::FromSeconds(60))
    }
    Write-Ok "$serviceName is running."
}
