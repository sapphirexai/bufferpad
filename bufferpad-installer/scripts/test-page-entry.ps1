param([Parameter(Mandatory=$true)][string]$BaseUrl, $AdminSession, $ReaderSession)
. (Join-Path $PSScriptRoot 'common.ps1')
Add-Type -AssemblyName System.Net.Http

function Read-Page {
    param([string]$Path, $Session)
    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.AllowAutoRedirect = $false
    if ($null -ne $Session) { $handler.CookieContainer = $Session.Cookies }
    $client = New-Object System.Net.Http.HttpClient($handler)
    try {
        $response = $client.GetAsync("$BaseUrl$Path").GetAwaiter().GetResult()
        return @{
            Status = [int]$response.StatusCode
            Location = [string]$response.Headers.Location
            Cache = [string]$response.Headers.CacheControl
            Body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        }
    } finally { $client.Dispose(); $handler.Dispose() }
}

$paths = @('/', '/index', '/summary', '/record', '/details', '/logs', '/settings/users', '/settings/devices', '/settings/plc-addresses', '/settings/lifespan', '/settings/install-positions', '/change-password', '/index.html')
foreach ($path in $paths) {
    $page = Read-Page $path
    if ($page.Status -ne 302 -or $page.Location -notmatch '/login$' -or $page.Cache -notmatch 'no-store') { Fail "Anonymous page entry was not intercepted: $path (status=$($page.Status), location=$($page.Location), cache=$($page.Cache))" }
}
$forged = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$forged.Cookies.Add((New-Object System.Net.Cookie('BUFFERPAD_SESSION', 'invalid-session', '/', ([Uri]$BaseUrl).Host)))
if ((Read-Page '/index' $forged).Status -ne 302) { Fail 'Invalid session must not access business documents.' }
$login = Read-Page '/login'
if ($login.Status -ne 200 -or $login.Body -notmatch '<div id=app>' -or $login.Cache -notmatch 'no-store') { Fail 'Login page or cache policy failed.' }
$config = Read-Page '/static/config.js'
if ($config.Status -ne 200 -or $config.Cache -notmatch 'no-store') { Fail 'Runtime configuration must not be cached.' }
if ((Read-Page '/_bufferpad_session').Status -ne 404) { Fail 'Internal authentication route must not be public.' }
if ((Read-Page '/api/auth/me').Status -ne 401) { Fail 'API must retain JSON 401 behavior.' }
foreach ($session in @($AdminSession, $ReaderSession)) {
    if ($null -eq $session) { continue }
    foreach ($path in @('/index', '/summary', '/record', '/change-password')) {
        $page = Read-Page $path $session
        if ($page.Status -ne 200 -or $page.Body -notmatch '<div id=app>' -or $page.Cache -notmatch 'no-store') { Fail "Authenticated page entry failed: $path" }
    }
}
foreach ($match in [regex]::Matches($login.Body, '(?:src|href)=(/static/[^ >]+)')) {
    if ((Read-Page $match.Groups[1].Value).Status -ne 200) { Fail 'Login page asset is unavailable anonymously.' }
}
Write-Ok 'Page entry, real session validation, public login assets and cache policy passed.'
