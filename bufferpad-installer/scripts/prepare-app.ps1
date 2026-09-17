param(
    [switch]$Build
)

. (Join-Path $PSScriptRoot 'common.ps1')

$installerHome = Get-InstallerHome
$repoRoot = (Resolve-Path (Join-Path $installerHome '..')).Path
$backendRoot = Join-Path $repoRoot 'wms-opc'
$frontendRoot = Join-Path $repoRoot 'bufferpad'

if ($Build) {
    $hslPath = Join-Path $backendRoot 'src\main\resources\lib\HslCommunication-3.4.0.jar'
    if (-not (Test-Path -LiteralPath $hslPath)) {
        Fail 'HSL JAR is not included. Follow wms-opc/src/main/resources/lib/README.md before building.'
    }
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
    Remove-DirectorySafe -Root $installerHome -Path $frontendTarget
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

Write-Step "Synchronizing authentication schema migration"
$authSchema = Join-Path $backendRoot 'src\main\resources\db\auth-schema.sql'
$authMigration = Join-Path $installerHome 'app\db\migrations\20260914_user_auth.sql'
Copy-Item -LiteralPath $authSchema -Destination $authMigration -Force
$firstLoginPolicy = Join-Path $backendRoot 'src\main\resources\db\auth-first-login-policy.sql'
Copy-Item -LiteralPath $firstLoginPolicy -Destination (Join-Path $installerHome 'app\db\migrations\20260914_user_first_login_optional.sql') -Force

Write-Step "Synchronizing scan log retention index migration"
Copy-Item -LiteralPath (Join-Path $backendRoot 'docs\sql\20260914_scan_log_retention_index.sql') -Destination (Join-Path $installerHome 'app\db\migrations\20260914_scan_log_retention_index.sql') -Force

Copy-Item -LiteralPath (Join-Path $backendRoot 'docs\sql\20260914_scan_log_operation_summary.sql') -Destination (Join-Path $installerHome 'app\db\migrations\20260914_scan_log_operation_summary.sql') -Force
Copy-Item -LiteralPath (Join-Path $backendRoot 'docs\sql\20260915_builtin_admin_session.sql') -Destination (Join-Path $installerHome 'app\db\migrations\20260915_builtin_admin_session.sql') -Force
