# Install Guide

## Recommended: install from Rider UI

1. Build the plugin:

   ```powershell
   .\scripts\package.ps1
   ```

2. Open Rider.
3. Go to `Settings` -> `Plugins`.
4. Click the gear icon.
5. Choose `Install Plugin from Disk...`.
6. Select:

   ```text
   build\distributions\csharp-stack-trace-tree-1.0.0.zip
   ```

7. Restart Rider.

## Script install

```powershell
.\scripts\install-local.ps1 -PluginZip ".\build\distributions\csharp-stack-trace-tree-1.0.0.zip"
```

If the Rider config directory cannot be found:

```powershell
.\scripts\install-local.ps1 `
  -PluginZip ".\build\distributions\csharp-stack-trace-tree-1.0.0.zip" `
  -RiderConfigDir "$env:APPDATA\JetBrains\Rider2025.3"
```

Restart Rider after installation.

## Uninstall

```powershell
.\scripts\uninstall-local.ps1
```
