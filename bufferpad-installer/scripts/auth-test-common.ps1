function Invoke-AuthCheck {
    param([string]$Method, [string]$Path, $Session, $Body, [int]$Expected = 200, [switch]$WithoutCsrf)
    $origin = ([Uri]$BaseUrl).GetLeftPart([UriPartial]::Authority)
    $headers = @{ Origin=$origin }
    if ($Method -notin @('GET', 'HEAD', 'OPTIONS') -and -not $WithoutCsrf) {
        $csrf = Invoke-RestMethod -Uri "$BaseUrl/auth/csrf" -Headers @{Origin=$origin} -WebSession $Session -TimeoutSec 10
        $headers['X-XSRF-TOKEN'] = $csrf.data.token
    }
    $parameters = @{ Uri="$BaseUrl$Path"; Method=$Method; WebSession=$Session; Headers=$headers; UseBasicParsing=$true; TimeoutSec=15 }
    if ($null -ne $Body) { $parameters.ContentType='application/json; charset=utf-8'; $parameters.Body=([Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Compress))) }
    try {
        $response = Invoke-WebRequest @parameters
        $status = [int]$response.StatusCode
        $content = $response.Content
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        $status = [int]$_.Exception.Response.StatusCode
        $content = ''
        if ($null -ne $_.ErrorDetails) {
            $content = $_.ErrorDetails.Message
        } else {
            $reader = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
            try { $content = $reader.ReadToEnd() } finally { $reader.Dispose() }
        }
    }
    if ($status -ne $Expected) { Fail "HTTP acceptance failed: $Method $Path expected $Expected, received $status. $content" }
    $payload = $null
    if ($content -and $content.TrimStart().StartsWith('{')) { $payload = $content | ConvertFrom-Json }
    if ($Expected -eq 200 -and $null -ne $payload -and $payload.PSObject.Properties['codeSuccess'] -and -not $payload.codeSuccess) {
        Fail "Business acceptance failed: $Method $Path : $($payload.msg)"
    }
    Write-Ok "$Method $Path -> $status"
    return $payload
}
