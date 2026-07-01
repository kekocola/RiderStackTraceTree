param(
    [string]$RiderPath
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot

function Find-RiderPath {
    param([string]$ExplicitPath)

    if ($ExplicitPath -and (Test-Path (Join-Path $ExplicitPath "product-info.json"))) {
        return (Resolve-Path $ExplicitPath).Path
    }

    $candidates = @(
        "C:\Program Files\JetBrains\JetBrains Rider 2025.3.3",
        "C:\Program Files\JetBrains\Rider"
    )

    $jetBrainsRoot = "C:\Program Files\JetBrains"
    if (Test-Path $jetBrainsRoot) {
        $candidates += Get-ChildItem $jetBrainsRoot -Directory -Filter "*Rider*" |
            Sort-Object Name -Descending |
            ForEach-Object { $_.FullName }
    }

    return $candidates | Where-Object { Test-Path (Join-Path $_ "product-info.json") } | Select-Object -First 1
}

function Ensure-JavaHome {
    param([string]$ResolvedRiderPath)

    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return
    }

    if ($ResolvedRiderPath) {
        $jbr = Join-Path $ResolvedRiderPath "jbr"
        if (Test-Path (Join-Path $jbr "bin\java.exe")) {
            $env:JAVA_HOME = $jbr
            $env:Path = "$jbr\bin;$env:Path"
            Write-Host "Using Rider bundled JBR: $jbr"
            return
        }
    }

    $java = Get-Command java -ErrorAction SilentlyContinue
    if (-not $java) {
        throw "Java 21 was not found. Install JDK 21 or pass -RiderPath to use Rider bundled JBR."
    }
}

$resolvedRider = Find-RiderPath $RiderPath
if ($resolvedRider) {
    Write-Host "Detected Rider: $resolvedRider"
} else {
    Write-Warning "Rider was not detected. Use -RiderPath or Gradle -PriderIdePath=..."
}

Ensure-JavaHome $resolvedRider

Push-Location $RepoRoot
try {
    if (-not (Test-Path ".\gradlew.bat")) {
        throw "gradlew.bat is missing."
    }
    & .\gradlew.bat --version
} finally {
    Pop-Location
}

Write-Host "Bootstrap completed."
