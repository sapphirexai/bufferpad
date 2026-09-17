param(
    [string]$SourceDir = 'C:\Users\YOUR_USER\Downloads\deploy'
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$packageDir = Join-Path $installerHome 'packages'

if (-not (Test-Path -LiteralPath $SourceDir)) {
    Fail "Source directory not found: $SourceDir"
}

New-Directory -Path $packageDir

$rules = @(
    @{
        Name = 'JDK 17 zip'
        Patterns = @('*jdk*17*.zip')
        Required = $true
    },
    @{
        Name = 'MySQL 8.0 zip'
        Patterns = @('mysql-8.0.*-winx64.zip')
        Required = $true
    },
    @{
        Name = 'nginx zip'
        Patterns = @('nginx*.zip')
        Required = $true
    },
    @{
        Name = 'WinSW x64'
        Patterns = @('WinSW-x64.exe', 'winsw-x64.exe')
        Required = $true
    },
    @{
        Name = 'VC++ Redistributable'
        Patterns = @('VC_redist.x64.exe')
        Required = $true
    }
)

foreach ($rule in $rules) {
    $matches = @()
    foreach ($pattern in $rule.Patterns) {
        $matches += @(Get-ChildItem -Path $SourceDir -Filter $pattern -File -ErrorAction SilentlyContinue)
    }
    $matches = @($matches | Sort-Object Length -Descending | Select-Object -First 1)

    if ($matches.Count -eq 0) {
        if ($rule.Required) {
            Fail "Missing $($rule.Name) under $SourceDir. Expected pattern(s): $($rule.Patterns -join ', ')"
        }
        Write-Warn "Optional package missing: $($rule.Name)"
        continue
    }

    $source = $matches[0].FullName
    $destination = Join-Path $packageDir $matches[0].Name
    Copy-Item -LiteralPath $source -Destination $destination -Force
    if ($destination -like '*.zip') {
        Test-ZipFile -Path $destination
    }
    Write-Ok "Collected $($rule.Name): $destination"
}

$partials = @(Get-ChildItem -Path $packageDir -Filter '*.download' -File -ErrorAction SilentlyContinue)
foreach ($partial in $partials) {
    Remove-Item -LiteralPath $partial.FullName -Force -ErrorAction SilentlyContinue
    Write-Warn "Removed partial download: $($partial.Name)"
}

Write-Ok "All required packages are collected under $packageDir"
