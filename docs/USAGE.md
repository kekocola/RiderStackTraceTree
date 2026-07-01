# Usage Guide

1. Open the target C# project in Rider.

   Example project root:

   ```text
   D:\Server2\pokeworld\Server\GameServer
   ```

2. Open `View` -> `Tool Windows` -> `Stack Trace Tree`.
3. Paste a C#/.NET stack trace into the input area.
4. Click `Parse`.
5. Expand the generated tree.
6. Click a stack frame to open its source file and line.

Example supported stack frame:

```text
at ET.Server.ActorHandleHelper.HandleIActorRequest(Int64 actorId, IActorRequest iActorRequest) in D:\Server2\pokeworld_dev\Server\GameServer\DotNet\Hotfix\Code\Module\Actor\ActorHandleHelper.cs:line 65
```

When the current project root is:

```text
D:\Server2\pokeworld\Server\GameServer
```

the plugin extracts:

```text
DotNet\Hotfix\Code\Module\Actor\ActorHandleHelper.cs
```

and navigates to:

```text
D:\Server2\pokeworld\Server\GameServer\DotNet\Hotfix\Code\Module\Actor\ActorHandleHelper.cs
```

## Path mappings

If automatic path matching fails, click `Settings` and add one mapping per line:

```text
D:\Server2\pokeworld_dev\Server\GameServer => D:\Server2\pokeworld\Server\GameServer
```

Mappings are applied before suffix matching.
