param(
    [Parameter(Mandatory = $true)]
    [string]$PluginZip,

    [string]$RiderConfigDir
)

$ErrorActionPreference = "Stop"

function Find-RiderPluginsDir {
    param([string]$ExplicitConfigDir)

    if ($ExplicitConfigDir) {
        return (Join-Path $ExplicitConfigDir "plugins")
    }

    $jetBrainsConfig = Join-Path $env:APPDATA "JetBrains"
    if (-not (Test-Path $jetBrainsConfig)) {
        throw "JetBrains config directory was not found. Pass -RiderConfigDir explicitly."
    }

    $latest = Get-ChildItem $jetBrainsConfig -Directory -Filter "Rider*" |
        Sort-Object Name -Descending |
        Select-Object -First 1

    if (-not $latest) {
        throw "No Rider config directory was found under $jetBrainsConfig. Start Rider once or pass -RiderConfigDir."
    }

    return (Join-Path $latest.FullName "plugins")
}

$zipPath = Resolve-Path $PluginZip
$pluginsDir = Find-RiderPluginsDir $RiderConfigDir
New-Item -ItemType Directory -Force -Path $pluginsDir | Out-Null
$pluginsDirResolved = (Resolve-Path $pluginsDir).Path.TrimEnd('\')

$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("csharp-stack-trace-tree-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

try {
    Expand-Archive -Path $zipPath -DestinationPath $tempDir -Force
    $pluginRoot = Get-ChildItem $tempDir -Directory | Select-Object -First 1
    if (-not $pluginRoot) {
        throw "The plugin zip did not contain a plugin root directory."
    }

    $target = Join-Path $pluginsDir $pluginRoot.Name
    if (Test-Path $target) {
        $targetResolved = (Resolve-Path $target).Path
        if (-not $targetResolved.StartsWith("$pluginsDirResolved\", [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove a path outside Rider plugins: $targetResolved"
        }
        Remove-Item -LiteralPath $target -Recurse -Force
    }

    Move-Item -LiteralPath $pluginRoot.FullName -Destination $target
    Write-Host "Installed plugin to: $target"
    Write-Host "Restart Rider to load the plugin."
} finally {
    if (Test-Path $tempDir) {
        Remove-Item -LiteralPath $tempDir -Recurse -Force
    }
}
