. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$testRoot = Join-Path $installerHome 'logs\uninstall-tests'
if (Test-Path -LiteralPath $testRoot) {
    Remove-DirectorySafe -Root $installerHome -Path $testRoot
}
New-Directory -Path $testRoot

function Invoke-UninstallSubprocess {
    param(
        [string[]]$Arguments
    )
    $script = Join-Path $installerHome 'scripts\uninstall-oneclick.ps1'
    $testServiceArgs = @(
        '-MysqlServiceName', 'BufferPadTestMySQL',
        '-BackendServiceName', 'BufferPadTestBackend',
        '-NginxServiceName', 'BufferPadTestNginx'
    )
    $allArgs = @('-ExecutionPolicy', 'Bypass', '-File', $script) + $testServiceArgs + $Arguments
    $output = & powershell @allArgs 2>&1
    return [pscustomobject]@{
        ExitCode = $LASTEXITCODE
        Output = ($output -join [Environment]::NewLine)
    }
}

Write-Step "Testing uninstall safety without marker"
$noMarkerRoot = Join-Path $testRoot 'no-marker-bufferpad'
New-Directory -Path $noMarkerRoot
Set-Content -LiteralPath (Join-Path $noMarkerRoot 'do-not-delete.txt') -Value 'sentinel' -Encoding ASCII
$result = Invoke-UninstallSubprocess -Arguments @('-DryRun', '-InstallRoot', $noMarkerRoot)
if ($result.ExitCode -eq 0) {
    Fail "Uninstall dry-run unexpectedly succeeded for a directory without marker."
}
if (-not (Test-Path -LiteralPath (Join-Path $noMarkerRoot 'do-not-delete.txt'))) {
    Fail "Safety test failed: sentinel file was removed from no-marker directory."
}
Write-Ok "No-marker directory was rejected and preserved."

Write-Step "Testing uninstall dry-run with marker"
$markerRoot = Join-Path $testRoot 'marked-bufferpad'
New-Directory -Path $markerRoot
Set-Content -LiteralPath (Join-Path $markerRoot '.bufferpad-install.json') -Value '{"product":"BufferPad"}' -Encoding ASCII
Set-Content -LiteralPath (Join-Path $markerRoot 'sentinel.txt') -Value 'sentinel' -Encoding ASCII
$result = Invoke-UninstallSubprocess -Arguments @('-DryRun', '-InstallRoot', $markerRoot)
if ($result.ExitCode -ne 0) {
    Write-Host $result.Output
    Fail "Uninstall dry-run failed for marked directory."
}
if (-not (Test-Path -LiteralPath (Join-Path $markerRoot 'sentinel.txt'))) {
    Fail "Dry-run should not remove files from marked directory."
}
Write-Ok "Marked directory dry-run succeeded and preserved files."

Write-Step "Testing unsafe root rejection"
$result = Invoke-UninstallSubprocess -Arguments @('-DryRun', '-InstallRoot', ([IO.Path]::GetPathRoot($installerHome)))
if ($result.ExitCode -eq 0) {
    Fail "Uninstall dry-run unexpectedly accepted drive root."
}
Write-Ok "Unsafe root was rejected."

Write-Ok "Uninstall tests completed."
exit 0
