# AGENTS.md

## Project

This repository contains a Rider frontend plugin named `C# Stack Trace Tree`.

The plugin lets users paste C#/.NET stack traces into a Rider Tool Window, renders stack frames as a tree, and navigates to source files and line numbers when the stack trace contains `in <path>:line <number>`.

## Agent Instructions

- Preserve the design document in [docs/DESIGN.md](docs/DESIGN.md). When behavior, architecture, path resolution, build flow, or installation flow changes materially, update that document.
- Record important project updates in the "Important Updates" section below. The user wrote `AGETNS.md` once, but the intended file is this standard `AGENTS.md`.
- Keep implementation aligned with the UI-only v1 scope unless the user explicitly asks for ReSharper backend symbol resolution.
- Prefer pure Kotlin parser/resolver logic with unit tests over embedding logic inside UI classes.
- Do not hand-create plugin zip archives. Use Gradle `buildPlugin`.
- Keep Windows PowerShell scripts as the primary supported automation path.
- Avoid deleting generated Gradle/Rider caches unless the user explicitly asks.

## Build And Verification

Useful commands:

```powershell
.\scripts\bootstrap.ps1
.\scripts\build.ps1
.\scripts\package.ps1
.\scripts\run-sandbox.ps1
```

If Rider is not auto-detected:

```powershell
.\scripts\build.ps1 -RiderPath "D:\Program Files\JetBrains\JetBrains Rider 2026.1"
```

Before reporting completion for build-related changes, try:

```powershell
.\scripts\build.ps1
```

If Gradle or dependency downloads are too slow or unavailable, say that clearly and include the last verified step.

## Important Updates

- 2026-07-01: Ignored IntelliJ Platform Gradle Plugin's generated `.intellijPlatform/` workspace directory.
- 2026-07-01: Changed plugin ID to `io.github.kokoc.csharp.stacktrace.tree` so JetBrains Plugin Verifier accepts the descriptor.
- 2026-07-01: Added JUnit 4 test dependency required by the IntelliJ Platform JUnit5 test listener.
- 2026-07-01: Aligned the Kotlin JVM toolchain with Rider 2025.3's Java 21 runtime.
- 2026-07-01: Fixed IntelliJ Platform Gradle Plugin 2.x Rider dependency configuration and made PowerShell build/package scripts stop cleanly on Gradle failures.
- 2026-07-01: Initial Rider plugin scaffold created with Kotlin, IntelliJ Platform Gradle Plugin 2.x, Tool Window UI, C# stack parser, source path resolver, navigation wrapper, settings dialog, tests, PowerShell scripts, and user docs.
- 2026-07-01: Added persistent design documentation, repository agent instructions, and `.gitignore`.
