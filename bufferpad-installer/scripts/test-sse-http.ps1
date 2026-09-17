param([Parameter(Mandatory=$true)][string]$BaseUrl, [Parameter(Mandatory=$true)]$AdminSession)
. (Join-Path $PSScriptRoot 'common.ps1')
. (Join-Path $PSScriptRoot 'auth-test-common.ps1')

$subscriber = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-AuthCheck POST '/auth/login' $subscriber @{username='reader';password='Acceptance-reader-123'} | Out-Null
$request = [Net.HttpWebRequest]::Create("$BaseUrl/sse/devicesStatus/1")
$request.CookieContainer = $subscriber.Cookies
$request.Timeout = 10000
$request.ReadWriteTimeout = 10000
$response = $null
$stream = $null
function Read-SseLine {
    $pendingLine = $stream.ReadLineAsync()
    if (-not $pendingLine.Wait(10000)) { throw 'SSE read timed out.' }
    return $pendingLine.Result
}
try {
    $response = $request.GetResponse()
    if ($response.ContentType -notlike 'text/event-stream*') { Fail 'SSE Content-Type is incorrect.' }
    $stream = New-Object IO.StreamReader($response.GetResponseStream())
    if ((Read-SseLine) -ne ':connected') { Fail 'SSE connection acknowledgment missing.' }
    Invoke-AuthCheck POST '/cushion/manualCushionInfo' $AdminSession @{workLine=1;qrCode='AUTH-SSE-ACCEPTANCE-001'} | Out-Null
    $found = $false
    for ($line = 0; $line -lt 20; $line++) {
        $text = Read-SseLine
        if ($null -eq $text) { break }
        if ($text -match 'data:.*AUTH-SSE-ACCEPTANCE-001') { $found = $true; break }
    }
    if (-not $found) { Fail 'The authenticated reader did not receive the live scan event.' }
    Invoke-AuthCheck POST '/auth/logout' $subscriber $null | Out-Null
    # Drain queued messages; the server must then finish this stream after revocation.
    $ended = $false
    for ($line = 0; $line -lt 40; $line++) { if ($null -eq (Read-SseLine)) { $ended = $true; break } }
    if (-not $ended) { Fail 'The SSE stream did not close after logout.' }
    Invoke-AuthCheck GET '/sse/devicesStatus/1' $subscriber $null 401 | Out-Null
    Write-Ok 'Real SSE delivered a business event to a reader and closed after logout.'
} finally {
    if ($null -ne $stream) { $stream.Dispose() }
    if ($null -ne $response) { $response.Dispose() }
    $request.Abort()
}
