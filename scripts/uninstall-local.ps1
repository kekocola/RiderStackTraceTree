param(
    [string]$RiderConfigDir
)

$ErrorActionPreference = "Stop"

function Find-RiderPluginsDir {
    param([string]$ExplicitConfigDir)

    if ($ExplicitConfigDir) {
        return (Join-Path $ExplicitConfigDir "plugins")
    }

    $jetBrainsConfig = Join-Path $env:APPDATA "JetBrains"
    $latest = Get-ChildItem $jetBrainsConfig -Directory -Filter "Rider*" |
        Sort-Object Name -Descending |
        Select-Object -First 1

    if (-not $latest) {
        throw "No Rider config directory was found under $jetBrainsConfig."
    }

    return (Join-Path $latest.FullName "plugins")
}

$pluginsDir = Find-RiderPluginsDir $RiderConfigDir
if (-not (Test-Path $pluginsDir)) {
    Write-Host "Plugins directory does not exist: $pluginsDir"
    return
}
$pluginsDirResolved = (Resolve-Path $pluginsDir).Path.TrimEnd('\')
$targets = @(
    Join-Path $pluginsDir "csharp-stack-trace-tree",
    Join-Path $pluginsDir "C# Stack Trace Tree"
)

foreach ($target in $targets) {
    if (Test-Path $target) {
        $targetResolved = (Resolve-Path $target).Path
        if (-not $targetResolved.StartsWith("$pluginsDirResolved\", [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove a path outside Rider plugins: $targetResolved"
        }
        Remove-Item -LiteralPath $target -Recurse -Force
        Write-Host "Removed: $target"
    }
}

Write-Host "Uninstall completed. Restart Rider if it is running."
