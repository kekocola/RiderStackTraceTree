# Rider C# Stack Trace Tree Design

## Goal

Build a Rider frontend plugin that accepts pasted C#/.NET stack traces, renders them as a navigable tree, and opens matching source files at the logged line number.

The first version is UI-only. It does not call the ReSharper backend to resolve symbols from method names. It focuses on stack frames that include `in <path>:line <number>`.

## User Flow

1. Open a C# project in Rider.
2. Open `View` -> `Tool Windows` -> `Stack Trace Tree`.
3. Paste a stack trace into the input area.
4. Click `Parse`, or enable automatic parsing in Settings.
5. Expand the generated tree.
6. Click a stack frame to navigate to the source file and line.

## Plugin Structure

- `StackTraceToolWindowFactory`: registers and creates the Rider Tool Window.
- `StackTracePanel`: owns the paste area, toolbar, stack tree, status text, settings dialog, and click handling.
- `CSharpStackTraceParser`: parses exceptions, inner exception markers, stack frames, and raw text lines.
- `SourcePathResolver`: maps paths from logs to files inside the currently opened Rider project.
- `StackFrameNavigator`: opens resolved files through IntelliJ Platform navigation APIs.
- `PluginSettingsState`: persists auto-parse and path mapping settings.

## Stack Frame Parsing

Supported frame shape:

```text
at Namespace.Type.Method(args) in D:\path\File.cs:line 65
```

Parsed fields:

- `rawLine`
- `method`
- `absolutePathFromLog`
- `fileName`
- `lineNumber`
- `innerExceptionLevel`
- `resolvedPath`
- `resolveStatus`

Unrecognized lines are preserved as text nodes, so partial or mixed logs remain readable.

## Path Resolution

Resolution order is fixed:

1. Direct absolute path match.
2. User-configured path mappings.
3. Project-root marker match.
4. Longest suffix match.
5. `NotFound`.

Example:

```text
Log path:
D:\Server2\pokeworld_dev\Server\GameServer\DotNet\Hotfix\Code\Module\Actor\ActorHandleHelper.cs

Opened project root:
D:\Server2\pokeworld\Server\GameServer

Extracted relative path:
DotNet\Hotfix\Code\Module\Actor\ActorHandleHelper.cs

Resolved local path:
D:\Server2\pokeworld\Server\GameServer\DotNet\Hotfix\Code\Module\Actor\ActorHandleHelper.cs
```

When multiple project roots contain the same suffix, the frame is marked as ambiguous and the user chooses from a candidate list.

## UI Behavior

Frame labels include method, file, line, and resolution status:

```text
ActorHandleHelper.HandleIActorRequest(...)  ActorHandleHelper.cs:65
ActorHandleHelper.HandleIActorRequest(...)  ActorHandleHelper.cs:65 [not found]
ActorHandleHelper.HandleIActorRequest(...)  ActorHandleHelper.cs:65 [multiple matches]
```

Click behavior:

- `Resolved`: open file and navigate to the line.
- `Ambiguous`: show candidate files, then navigate after user selection.
- `NotFound`: show attempted paths and suggest using Settings path mappings.

## Toolchain

The project uses:

- Kotlin JVM
- Java 21 toolchain, matching Rider 2025.3 / IntelliJ Platform 253
- IntelliJ Platform Gradle Plugin 2.x
- Rider platform type `RD`
- Gradle Wrapper
- PowerShell scripts for bootstrap, build, sandbox run, package, install, and uninstall

The scripts prefer Rider's bundled JetBrains Runtime when `java` is not already on `PATH`.

## Test Coverage

Unit tests cover:

- Standard C# stack frame parsing.
- Windows drive-letter paths.
- `:line <number>` extraction.
- Project-root marker path extraction.
- User path mappings.
- Ambiguous matches.
- Missing files.

Manual validation should cover:

- Tool Window visibility in Rider.
- Paste and parse behavior.
- Clicking `ActorHandleHelper.cs:65` opens the expected file and line.
- Installing the Gradle-built zip through Rider's `Install Plugin from Disk...`.

## Known Limits

- Frames without `in <path>:line <number>` are not symbol-resolved.
- ReSharper backend symbol lookup is intentionally out of scope for v1.
- Windows is the primary target environment for scripts and path examples.
- The plugin zip must be produced by Gradle `buildPlugin`; manually compressed zips are not supported.
