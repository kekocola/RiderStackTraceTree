param(
    [string]$RiderPath
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot "bootstrap.ps1") -RiderPath $RiderPath

$gradleArgs = @("runIde")
if ($RiderPath) {
    $gradleArgs += "-PriderIdePath=$RiderPath"
}

Push-Location $RepoRoot
try {
    & .\gradlew.bat @gradleArgs
} finally {
    Pop-Location
}
