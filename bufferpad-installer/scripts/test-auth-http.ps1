param([Parameter(Mandatory=$true)][string]$BaseUrl)
. (Join-Path $PSScriptRoot 'common.ps1')
. (Join-Path $PSScriptRoot 'auth-test-common.ps1')

$anonymous = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$admin = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$secondAdmin = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$reader = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$adminPassword = 'Acceptance-admin-123'
$readerPassword = 'Acceptance-reader-123'

Invoke-AuthCheck GET '/opcConfig/page' $anonymous $null 401 | Out-Null
Invoke-AuthCheck GET '/sse/devicesStatus/1' $anonymous $null 401 | Out-Null
Invoke-AuthCheck POST '/auth/login' $anonymous @{username='admin';password='example-admin-password'} 403 -WithoutCsrf | Out-Null
$first = Invoke-AuthCheck POST '/auth/login' $admin @{username='admin';password='example-admin-password'}
if ($first.data.mustChangePassword) { Fail 'Initial administrator must be able to use the application immediately.' }
Invoke-AuthCheck GET '/users' $admin $null | Out-Null
Invoke-AuthCheck POST '/auth/password' $admin @{oldPassword='wrong';newPassword=$adminPassword} 400 | Out-Null
Invoke-AuthCheck POST '/auth/password' $admin @{oldPassword='example-admin-password';newPassword=$adminPassword} | Out-Null
Invoke-AuthCheck GET '/auth/me' $admin $null 401 | Out-Null
Invoke-AuthCheck POST '/auth/login' $admin @{username='admin';password=$adminPassword} | Out-Null
Invoke-AuthCheck POST '/auth/login' $secondAdmin @{username='admin';password=$adminPassword} | Out-Null
Invoke-AuthCheck POST '/users' $admin @{username='reader';password='Reader-initial-123';role='USER'} | Out-Null
$firstReader = Invoke-AuthCheck POST '/auth/login' $reader @{username='reader';password='Reader-initial-123'}
if ($firstReader.data.mustChangePassword) { Fail 'New reader must not be forced to change password.' }
Invoke-AuthCheck GET '/opcConfig/page' $reader $null | Out-Null
Invoke-AuthCheck GET '/users' $reader $null 403 | Out-Null
Invoke-AuthCheck POST '/auth/password' $reader @{oldPassword='Reader-initial-123';newPassword=$readerPassword} | Out-Null
Invoke-AuthCheck POST '/auth/login' $reader @{username='reader';password=$readerPassword} | Out-Null
& (Join-Path $PSScriptRoot 'test-page-entry.ps1') -BaseUrl ([Uri]$BaseUrl).GetLeftPart([UriPartial]::Authority) -AdminSession $admin -ReaderSession $reader
foreach ($path in @('/opcConfig/page','/device/devicesPage','/device/devicesStatus/1','/deviceInstallPositions/page','/plcAddr/page','/cushion/cushionsPage','/scanLogs','/operationEvents/recent','/options/deviceTypes')) {
    Invoke-AuthCheck GET $path $reader $null | Out-Null
}
Invoke-AuthCheck GET '/users' $reader $null 403 | Out-Null
Invoke-AuthCheck GET '/device/deviceConnections/1' $reader $null 403 | Out-Null
Invoke-AuthCheck GET '/device/scannerConnections' $reader $null 403 | Out-Null
Invoke-AuthCheck POST '/cushion/manualCushionInfo' $reader @{workLine=1;qrCode='AUTH-ACCEPTANCE-001'} 403 | Out-Null
Invoke-AuthCheck POST '/opcConfig' $reader @{id=1;cushionMaxUseCount=600} 403 | Out-Null
Invoke-AuthCheck POST '/opcConfig' $admin @{id=1;cushionMaxUseCount=600} | Out-Null
Invoke-AuthCheck POST '/cushion/manualCushionInfo' $admin @{workLine=1;qrCode='AUTH-ACCEPTANCE-001'} | Out-Null
$rows = Invoke-AuthCheck GET '/cushion/cushionsPage?currentPage=1&pageSize=10&qrCode=AUTH-ACCEPTANCE-001' $reader $null
$id = @($rows.data.data)[0].id
$csrf = Invoke-RestMethod -Uri "$BaseUrl/auth/csrf" -WebSession $reader
$export = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/cushion/cushions/excel" -Method Post -WebSession $reader -Headers @{'X-XSRF-TOKEN'=$csrf.data.token} -ContentType 'application/json' -Body "[$id]" -TimeoutSec 20
if ([int]$export.StatusCode -ne 200 -or $export.RawContentLength -lt 100) { Fail 'Reader Excel export failed.' }
Write-Ok 'Reader can export a real Excel workbook.'
& (Join-Path $PSScriptRoot 'test-sse-http.ps1') -BaseUrl $BaseUrl -AdminSession $admin
$users = Invoke-AuthCheck GET '/users' $admin $null
$readerId = @($users.data | Where-Object username -eq 'reader')[0].id
$adminId = @($users.data | Where-Object username -eq 'admin')[0].id
Invoke-AuthCheck PUT "/users/$adminId" $admin @{role='USER';enabled=$true} 400 | Out-Null
Invoke-AuthCheck PUT "/users/$readerId" $admin @{role='USER';enabled=$false} | Out-Null
Invoke-AuthCheck GET '/auth/me' $reader $null 401 | Out-Null
Invoke-AuthCheck POST '/auth/login' $reader @{username='reader';password=$readerPassword} 401 | Out-Null
Invoke-AuthCheck PUT "/users/$readerId" $admin @{role='USER';enabled=$true} | Out-Null
Invoke-AuthCheck POST "/users/$readerId/password" $admin @{password='Reader-reset-123'} | Out-Null
Invoke-AuthCheck POST '/auth/login' $reader @{username='reader';password='Reader-reset-123'} | Out-Null
Invoke-AuthCheck GET '/opcConfig/page' $reader $null 403 | Out-Null
Invoke-AuthCheck POST '/auth/password' $reader @{oldPassword='Reader-reset-123';newPassword=$readerPassword} | Out-Null
Invoke-AuthCheck POST '/auth/logout' $secondAdmin $null | Out-Null
Invoke-AuthCheck GET '/auth/me' $secondAdmin $null 401 | Out-Null
Invoke-AuthCheck GET '/auth/me' $admin $null | Out-Null
Write-Ok 'Authentication HTTP acceptance passed.'
# Returned only to the calling runtime test, to verify that this session survives a backend restart.
return $admin
