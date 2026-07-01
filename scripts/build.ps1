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
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE."
    }

    $distributionPath = ".\build\distributions"
    if (-not (Test-Path $distributionPath)) {
        Write-Warning "Build completed, but no distribution directory was found."
        return
    }

    Get-ChildItem $distributionPath -Filter "*.zip" | ForEach-Object {
        Write-Host "Plugin package: $($_.FullName)"
    }
} finally {
    Pop-Location
}
