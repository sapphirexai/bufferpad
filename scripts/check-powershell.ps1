$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $files = @(git -c core.quotepath=false ls-files -- '*.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Unable to list tracked PowerShell scripts.' }
    foreach ($file in $files) {
        $tokens = $null
        $parseErrors = $null
        [System.Management.Automation.Language.Parser]::ParseFile(
            (Join-Path $projectRoot $file), [ref]$tokens, [ref]$parseErrors
        ) | Out-Null
        if ($parseErrors.Count -gt 0) {
            throw "PowerShell syntax error in ${file}: $($parseErrors.Message -join '; ')"
        }
    }
    Write-Host "PASS: parsed $($files.Count) PowerShell files. No installer or service actions executed."
} finally {
    Pop-Location
}
