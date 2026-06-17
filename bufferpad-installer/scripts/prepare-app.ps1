param(
    [switch]$Build
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$repoRoot = (Resolve-Path (Join-Path $installerHome '..')).Path
$backendRoot = Join-Path $repoRoot 'wms-opc'
$frontendRoot = Join-Path $repoRoot 'bufferpad'

if ($Build) {
    Write-Step "Building backend jar"
    Push-Location $backendRoot
    try {
        Invoke-Checked -FilePath 'mvn' -Arguments @('-q', '-DskipTests', 'package') -ErrorMessage 'Backend Maven build failed.'
    } finally {
        Pop-Location
    }

    Write-Step "Building frontend dist"
    Push-Location $frontendRoot
    try {
        Invoke-Checked -FilePath 'npm' -Arguments @('run', 'build') -ErrorMessage 'Frontend npm build failed.'
    } finally {
        Pop-Location
    }
}

$jar = @(Get-ChildItem -Path (Join-Path $backendRoot 'target') -Filter 'opc-*.jar' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1)
if ($jar.Count -eq 0) {
    Fail "Backend jar not found. Run wms-opc Maven package first or call scripts\prepare-app.ps1 -Build."
}

$dist = Join-Path $frontendRoot 'dist'
if (-not (Test-Path -LiteralPath (Join-Path $dist 'index.html'))) {
    Fail "Frontend dist not found. Run bufferpad npm build first or call scripts\prepare-app.ps1 -Build."
}

$backendTarget = Join-Path $installerHome 'app\backend\opc.jar'
$frontendTarget = Join-Path $installerHome 'app\frontend\dist'

Write-Step "Copying backend jar"
New-Directory -Path (Split-Path -Parent $backendTarget)
Copy-Item -LiteralPath $jar[0].FullName -Destination $backendTarget -Force
Write-Ok "Copied $($jar[0].Name) to $backendTarget"

Write-Step "Copying frontend dist"
if (Test-Path -LiteralPath $frontendTarget) {
    Remove-Item -LiteralPath $frontendTarget -Recurse -Force
}
New-Directory -Path $frontendTarget
Copy-Item -Path (Join-Path $dist '*') -Destination $frontendTarget -Recurse -Force
Write-Ok "Copied frontend dist to $frontendTarget"

$dbPath = Join-Path $installerHome 'app\db\wms_opc.sql'
if (-not (Test-Path -LiteralPath $dbPath)) {
    Write-Warn "Database dump is not present yet: $dbPath"
    Write-Warn "Put your exported test database dump there before a full installation."
}

Write-Ok "Application artifacts are ready."
