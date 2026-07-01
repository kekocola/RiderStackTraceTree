param(
    [string]$RiderPath
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot "bootstrap.ps1") -RiderPath $RiderPath

$gradleArgs = @("clean", "test", "buildPlugin", "verifyPlugin")
if ($RiderPath) {
    $gradleArgs += "-PriderIdePath=$RiderPath"
}

Push-Location $RepoRoot
try {
    & .\gradlew.bat @gradleArgs
    Get-ChildItem ".\build\distributions" -Filter "*.zip" | ForEach-Object {
        Write-Host "Plugin package: $($_.FullName)"
    }
} finally {
    Pop-Location
}
