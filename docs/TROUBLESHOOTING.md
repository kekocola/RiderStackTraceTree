# Troubleshooting

## Plugin zip does not install

Use the zip produced by Gradle:

```powershell
.\scripts\package.ps1
```

Do not create the plugin zip manually with `Compress-Archive`.

## Source file is not found

Confirm that the project root opened in Rider is the source root you expect. For the sample path, Rider should have this root available:

```text
D:\Server2\pokeworld\Server\GameServer
```

If the log path uses a different checkout name, add a path mapping in the plugin settings.

## Multiple files match

The tree node shows `[multiple matches]`. Click it and choose the correct file from the dialog.

## Click does not open the file

Wait until Rider finishes project indexing, then try again. If it still fails, open `Help` -> `Show Log in Explorer` and check the IDE log.

## Build fails because Java is missing

Install JDK 17+ or pass a Rider path so the scripts can use Rider's bundled JBR:

```powershell
.\scripts\build.ps1 -RiderPath "D:\Program Files\JetBrains\JetBrains Rider 2026.1"
```

## Gradle cannot download dependencies

Check access to:

- `https://plugins.gradle.org`
- `https://cache-redirector.jetbrains.com`
- `https://www.jetbrains.com`
- `https://repo.maven.apache.org`
