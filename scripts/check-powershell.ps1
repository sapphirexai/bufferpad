$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $files = @(git -c core.quotepath=false ls-files -- '*.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Unable to list tracked PowerShell scripts.' }
    foreach ($file in $files) {
        $tokens = $null
        $parseErrors = $null
        # Windows PowerShell 5.1 otherwise reads BOM-less UTF-8 as the local
        # ANSI code page. Parse the repository's declared text encoding explicitly.
        $fullPath = Join-Path $projectRoot $file
        $source = [System.IO.File]::ReadAllText($fullPath, [System.Text.Encoding]::UTF8)
        [System.Management.Automation.Language.Parser]::ParseInput(
            $source, $fullPath, [ref]$tokens, [ref]$parseErrors
        ) | Out-Null
        if ($parseErrors.Count -gt 0) {
            throw "PowerShell syntax error in ${file}: $($parseErrors.Message -join '; ')"
        }
    }
    Write-Host "PASS: parsed $($files.Count) PowerShell files as UTF-8. No installer or service actions executed."
} finally {
    Pop-Location
}
