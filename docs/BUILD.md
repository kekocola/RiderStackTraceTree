# Build Guide

## Requirements

- Windows PowerShell 5+ or PowerShell 7+
- Rider installed locally
- Java 17+

If Java is not on `PATH`, the scripts try to use Rider's bundled JetBrains Runtime from `jbr\bin\java.exe`.

## Bootstrap

```powershell
.\scripts\bootstrap.ps1
```

If Rider is not auto-detected:

```powershell
.\scripts\bootstrap.ps1 -RiderPath "D:\Program Files\JetBrains\JetBrains Rider 2026.1"
```

## Build and verify

```powershell
.\scripts\build.ps1
```

With an explicit Rider path:

```powershell
.\scripts\build.ps1 -RiderPath "D:\Program Files\JetBrains\JetBrains Rider 2026.1"
```

The plugin zip is generated under:

```text
build\distributions\csharp-stack-trace-tree-<version>.zip
```

## Developer sandbox

```powershell
.\scripts\run-sandbox.ps1
```

This starts a sandbox Rider instance with the plugin loaded.
