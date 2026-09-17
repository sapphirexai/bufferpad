param([switch]$SkipTests, [switch]$PublicRepositories)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$pom = Join-Path $projectRoot 'wms-opc\pom.xml'
$hsl = Join-Path $projectRoot 'wms-opc\src\main\resources\lib\HslCommunication-3.4.0.jar'
if (-not (Test-Path -LiteralPath $hsl -PathType Leaf)) {
    throw 'HslCommunication-3.4.0.jar is not distributed with this repository. Obtain an appropriately licensed copy and follow wms-opc/src/main/resources/lib/README.md before building.'
}
$arguments = @('-f', $pom, '-Pprod', 'clean', 'package')
if ($PublicRepositories) {
    $settings = Join-Path $PSScriptRoot 'maven-public-settings.xml'
    $arguments += @('-s', $settings, '-gs', $settings)
}
if ($SkipTests) { $arguments += '-DskipTests' }
& mvn @arguments
if ($LASTEXITCODE -ne 0) { throw "Backend build failed (Maven exit code $LASTEXITCODE)." }
