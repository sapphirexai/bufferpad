param(
    [Parameter(Mandatory = $true)]
    [string]$ScriptPath,
    [ValidateSet('Install', 'Uninstall', 'Run')]
    [string]$Action = 'Run'
)

$ErrorActionPreference = 'Stop'

function ConvertFrom-CodePoint {
    param([int[]]$CodePoints)
    return (-join ($CodePoints | ForEach-Object { [char]$_ }))
}

function Get-UiText {
    param([string]$Key)

    switch ($Key) {
        'InstallSuccess' { return ConvertFrom-CodePoint @(0x5B89, 0x88C5, 0x6210, 0x529F) }
        'InstallFailed' { return ConvertFrom-CodePoint @(0x5B89, 0x88C5, 0x5931, 0x8D25) }
        'UninstallSuccess' { return ConvertFrom-CodePoint @(0x5378, 0x8F7D, 0x6210, 0x529F) }
        'UninstallFailed' { return ConvertFrom-CodePoint @(0x5378, 0x8F7D, 0x5931, 0x8D25) }
        'RunSuccess' { return ConvertFrom-CodePoint @(0x6267, 0x884C, 0x6210, 0x529F) }
        'RunFailed' { return ConvertFrom-CodePoint @(0x6267, 0x884C, 0x5931, 0x8D25) }
        'PressAnyKey' { return ConvertFrom-CodePoint @(0x6309, 0x4EFB, 0x610F, 0x952E, 0x9000, 0x51FA, 0x002E, 0x002E, 0x002E) }
        'PressEnter' { return ConvertFrom-CodePoint @(0x6309, 0x0020, 0x0045, 0x006E, 0x0074, 0x0065, 0x0072, 0x0020, 0x952E, 0x9000, 0x51FA) }
        'ScriptMissing' { return ConvertFrom-CodePoint @(0x811A, 0x672C, 0x4E0D, 0x5B58, 0x5728, 0xFF1A) }
        'ExitCode' { return ConvertFrom-CodePoint @(0xFF0C, 0x9000, 0x51FA, 0x7801, 0xFF1A) }
        'SeeErrorAbove' { return ConvertFrom-CodePoint @(0x3002, 0x8BF7, 0x67E5, 0x770B, 0x4E0A, 0x65B9, 0x65E5, 0x5FD7, 0x4E2D, 0x7684, 0x9519, 0x8BEF, 0x539F, 0x56E0, 0x3002) }
        'Colon' { return ConvertFrom-CodePoint @(0xFF1A) }
        default { return $Key }
    }
}

function Wait-AnyKey {
    Write-Host ''
    Write-Host (Get-UiText 'PressAnyKey') -ForegroundColor Cyan
    try {
        $null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
    } catch {
        try {
            $null = [Console]::ReadKey($true)
        } catch {
            Read-Host (Get-UiText 'PressEnter') | Out-Null
        }
    }
}

function Get-ResultMessages {
    switch ($Action) {
        'Install' {
            return @{
                Success = Get-UiText 'InstallSuccess'
                Failed = Get-UiText 'InstallFailed'
            }
        }
        'Uninstall' {
            return @{
                Success = Get-UiText 'UninstallSuccess'
                Failed = Get-UiText 'UninstallFailed'
            }
        }
        default {
            return @{
                Success = Get-UiText 'RunSuccess'
                Failed = Get-UiText 'RunFailed'
            }
        }
    }
}

$messages = Get-ResultMessages
$exitCode = 0

try {
    if (-not (Test-Path -LiteralPath $ScriptPath)) {
        throw ((Get-UiText 'ScriptMissing') + $ScriptPath)
    }

    $powershellExe = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
    & $powershellExe -NoProfile -ExecutionPolicy Bypass -File $ScriptPath
    $exitCode = [int]$global:LASTEXITCODE

    Write-Host ''
    if ($exitCode -eq 0) {
        Write-Host $messages.Success -ForegroundColor Green
    } else {
        Write-Host ($messages.Failed + (Get-UiText 'ExitCode') + $exitCode + (Get-UiText 'SeeErrorAbove')) -ForegroundColor Red
    }
} catch {
    $exitCode = 1
    Write-Host ''
    Write-Host ($messages.Failed + (Get-UiText 'Colon') + $_.Exception.Message) -ForegroundColor Red
} finally {
    Wait-AnyKey
}

exit $exitCode
