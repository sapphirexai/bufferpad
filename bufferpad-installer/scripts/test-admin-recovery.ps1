param(
    [Parameter(Mandatory=$true)][string]$BaseUrl,
    [Parameter(Mandatory=$true)][string]$RuntimeRoot
)
. (Join-Path $PSScriptRoot 'common.ps1')
. (Join-Path $PSScriptRoot 'auth-test-common.ps1')

# This acceptance test is restricted to the disposable runtime created by test-runtime-smoke.
$expectedRoot = [IO.Path]::GetFullPath((Join-Path (Get-InstallerHome) 'logs\runtime-smoke'))
if ([IO.Path]::GetFullPath($RuntimeRoot).TrimEnd('\') -ne $expectedRoot.TrimEnd('\')) { Fail 'Recovery acceptance requires the temporary runtime directory.' }
$current = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-AuthCheck POST '/auth/login' $current @{username='admin';password='Acceptance-admin-123'} | Out-Null

$maintenanceScript = Join-Path $RuntimeRoot 'reset-admin-password.ps1'
$tokens = @{
    JAVA_EXE = Join-Path $RuntimeRoot 'java\bin\java.exe'
    BACKEND_JAR = Join-Path $RuntimeRoot 'backend\opc.jar'
    CONF_DIR = Convert-ToConfigPath (Join-Path $RuntimeRoot 'conf')
}
Write-RenderedTemplate -TemplatePath (Join-Path (Get-InstallerHome) 'config\reset-admin-password.ps1.template') -DestinationPath $maintenanceScript -Tokens $tokens
# Supply a disposable test secret to the real rendered maintenance script without a prompt.
function Read-Host { param($Prompt, [switch]$AsSecureString) ConvertTo-SecureString 'Recovery-temporary-123' -AsPlainText -Force }
$recoveryLog = Join-Path $RuntimeRoot 'logs\admin-recovery.log'
& $maintenanceScript *> $recoveryLog
if ($LASTEXITCODE -ne 0) { Fail "Recovery process failed. See $recoveryLog" }
$logText = Get-Content -LiteralPath $recoveryLog -Raw
if ($logText -notmatch 'Administrator recovered' -or $logText -match 'Tomcat started|Netty|连接扫码器') { Fail 'Recovery must finish without starting web or device listeners.' }
if (Test-Path Env:BUFFERPAD_RECOVERY_PASSWORD) { Fail 'Recovery password was not removed from process environment.' }
Invoke-AuthCheck GET '/auth/me' $current $null 401 | Out-Null
$recovered = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$login = Invoke-AuthCheck POST '/auth/login' $recovered @{username='admin';password='Recovery-temporary-123'}
if (-not $login.data.mustChangePassword) { Fail 'Recovered administrator must change password.' }
Invoke-AuthCheck GET '/users' $recovered $null 403 | Out-Null
Invoke-AuthCheck POST '/auth/password' $recovered @{oldPassword='Recovery-temporary-123';newPassword='Acceptance-admin-123'} | Out-Null
Invoke-AuthCheck POST '/auth/login' $recovered @{username='admin';password='Acceptance-admin-123'} | Out-Null
Invoke-AuthCheck GET '/users' $recovered $null | Out-Null
Write-Ok 'Rendered administrator recovery script passed against the real temporary MySQL database.'
