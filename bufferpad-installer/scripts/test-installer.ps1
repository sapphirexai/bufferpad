param(
    [switch]$CheckPackages
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome

Write-Step "PowerShell syntax check"
$scripts = Get-ChildItem -Path (Join-Path $installerHome 'scripts') -Filter '*.ps1' -File
foreach ($script in $scripts) {
    $tokens = $null
    $errors = $null
    [System.Management.Automation.Language.Parser]::ParseFile($script.FullName, [ref]$tokens, [ref]$errors) | Out-Null
    if ($errors.Count -gt 0) {
        $message = ($errors | ForEach-Object { "$($_.Extent.StartLineNumber): $($_.Message)" }) -join '; '
        Fail "Syntax error in $($script.FullName): $message"
    }
    Write-Ok "Syntax OK: $($script.Name)"
}

Write-Step "Application artifact check"
& (Join-Path $installerHome 'scripts\prepare-app.ps1')

if ($CheckPackages) {
    Write-Step "Package check"
    $config = Read-InstallConfig -InstallerHome $installerHome -InstallRootOverride $null
    foreach ($item in @(
        @('JDK 17', [string]$config['JavaPackagePattern'], 'zip'),
        @('MySQL', [string]$config['MysqlPackagePattern'], 'zip'),
        @('nginx', [string]$config['NginxPackagePattern'], 'zip'),
        @('WinSW', [string]$config['WinSWPackagePattern'], 'exe'),
        @('VC++ Redistributable', [string]$config['VCRedistPattern'], 'exe')
    )) {
        $pkg = Find-Package -InstallerHome $installerHome -Pattern $item[1] -DisplayName $item[0]
        if ($item[2] -eq 'zip') {
            Test-ZipFile -Path $pkg
        }
        Write-Ok "$($item[0]) package OK: $pkg"
    }
}

Write-Step "Dry-run installation check"
$config = Read-InstallConfig -InstallerHome $installerHome -InstallRootOverride $null
$mysqlPort = [int]$config['MysqlPort']
$backendPort = [int]$config['BackendPort']
$frontendPort = [int]$config['FrontendPort']
if ($null -ne (Get-ListeningPortOwner -Port $mysqlPort)) {
    $mysqlPort = Get-FreeTcpPort -StartPort 13306
    Write-Warn "Default MySQL port is occupied. Dry-run will use $mysqlPort."
}
if ($null -ne (Get-ListeningPortOwner -Port $backendPort)) {
    $backendPort = Get-FreeTcpPort -StartPort 19001
    Write-Warn "Default backend port is occupied. Dry-run will use $backendPort."
}
if ($null -ne (Get-ListeningPortOwner -Port $frontendPort)) {
    $frontendPort = Get-FreeTcpPort -StartPort 18088
    Write-Warn "Default frontend port is occupied. Dry-run will use $frontendPort."
}
& (Join-Path $installerHome 'scripts\install.ps1') -DryRun -SkipPackageDownload -SkipDbImport -MysqlPort $mysqlPort -BackendPort $backendPort -FrontendPort $frontendPort
if ($LASTEXITCODE -ne 0) {
    Fail "Dry-run install failed."
}

Write-Step "Service auto-start configuration check"
$dryRunDir = Join-Path $installerHome 'logs\dry-run'
foreach ($serviceXmlName in @('backend-service.xml', 'nginx-service.xml')) {
    $serviceXmlPath = Join-Path $dryRunDir $serviceXmlName
    if (-not (Test-Path -LiteralPath $serviceXmlPath)) {
        Fail "Dry-run service XML was not rendered: $serviceXmlPath"
    }
    [xml]$serviceXml = Get-Content -LiteralPath $serviceXmlPath -Raw
    if ($serviceXml.service.startmode -ne 'Automatic') {
        Fail "$serviceXmlName startmode must be Automatic."
    }
    if ($serviceXml.service.delayedAutoStart -ne 'true') {
        Fail "$serviceXmlName delayedAutoStart must be true."
    }
    Write-Ok "$serviceXmlName is configured for Automatic delayed start."
}

Write-Step "Backend management script rendering check"
$dryRunBackendDir = Join-Path $dryRunDir 'backend'
foreach ($scriptName in @(
    'reset-admin-password.ps1',
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
    $scriptPath = Join-Path $dryRunBackendDir $scriptName
    if (-not (Test-Path -LiteralPath $scriptPath)) {
        Fail "Dry-run backend management script was not rendered: $scriptPath"
    }
    if ($scriptName -like '*.ps1') {
        $tokens = $null
        $errors = $null
        [System.Management.Automation.Language.Parser]::ParseFile($scriptPath, [ref]$tokens, [ref]$errors) | Out-Null
        if ($errors.Count -gt 0) {
            $message = ($errors | ForEach-Object { "$($_.Extent.StartLineNumber): $($_.Message)" }) -join '; '
            Fail "Rendered backend management script has syntax errors: $scriptPath $message"
        }
    }
    Write-Ok "$scriptName rendered."
}

Write-Step "Uninstall safety check"
& (Join-Path $installerHome 'scripts\test-uninstall.ps1')
if ($LASTEXITCODE -ne 0) {
    Fail "Uninstall safety check failed."
}

Write-Ok "Installer test completed."
