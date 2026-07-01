package com.example.rider.stacktrace.tree.resolution

import com.example.rider.stacktrace.tree.settings.PluginSettingsState
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SourcePathResolverTest {
    @Test
    fun extractsRelativePathFromProjectRootMarker() {
        val temp = Files.createTempDirectory("stack-trace-tree")
        val root = temp.resolve("Server2").resolve("pokeworld").resolve("Server").resolve("GameServer")
        val file = root.resolve("DotNet").resolve("Hotfix").resolve("Code").resolve("Module").resolve("Actor").resolve("ActorHandleHelper.cs")
        file.parent.createDirectories()
        file.createFile()
        val remoteRootName = "stack-trace-tree-remote-${temp.fileName}"

        val resolver = SourcePathResolver()
        val resolution = resolver.resolve(
            """Z:\$remoteRootName\Server2\pokeworld_dev\Server\GameServer\DotNet\Hotfix\Code\Module\Actor\ActorHandleHelper.cs""",
            listOf(root),
            emptyList(),
        )

        assertIs<PathResolution.Resolved>(resolution)
        assertEquals(file, resolution.path)
        assertEquals(ResolutionStrategy.ProjectRootMarker, resolution.strategy)
    }

    @Test
    fun mappingHasPriorityOverSuffixMatching() {
        val temp = Files.createTempDirectory("stack-trace-tree")
        val mappedRoot = temp.resolve("mapped")
        val file = mappedRoot.resolve("DotNet").resolve("Hotfix").resolve("File.cs")
        file.parent.createDirectories()
        file.createFile()

        val resolver = SourcePathResolver()
        val resolution = resolver.resolve(
            """D:\old\DotNet\Hotfix\File.cs""",
            listOf(temp.resolve("other-root")),
            listOf(PluginSettingsState.PathMapping("""D:\old""", mappedRoot.toString())),
        )

        assertIs<PathResolution.Resolved>(resolution)
        assertEquals(file, resolution.path)
        assertEquals(ResolutionStrategy.Mapping, resolution.strategy)
    }

    @Test
    fun reportsAmbiguousWhenMultipleRootsContainSameSuffix() {
        val temp = Files.createTempDirectory("stack-trace-tree")
        val rootA = temp.resolve("rootA")
        val rootB = temp.resolve("rootB")
        val fileA = rootA.resolve("Code").resolve("File.cs")
        val fileB = rootB.resolve("Code").resolve("File.cs")
        fileA.parent.createDirectories()
        fileB.parent.createDirectories()
        fileA.createFile()
        fileB.createFile()

        val resolver = SourcePathResolver()
        val resolution = resolver.resolve("""D:\build\Code\File.cs""", listOf(rootA, rootB), emptyList())

        assertIs<PathResolution.Ambiguous>(resolution)
        assertEquals(2, resolution.candidates.size)
    }

    @Test
    fun reportsNotFoundWhenNoCandidateExists() {
        val temp = Files.createTempDirectory("stack-trace-tree")

        val resolver = SourcePathResolver()
        val resolution = resolver.resolve("""D:\missing\Code\File.cs""", listOf(temp), emptyList())

        assertIs<PathResolution.NotFound>(resolution)
    }
}
