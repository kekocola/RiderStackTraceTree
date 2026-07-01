package com.example.rider.stacktrace.tree.resolution

import com.example.rider.stacktrace.tree.settings.PluginSettingsState
import java.nio.file.Files
import java.nio.file.Path

class SourcePathResolver(
    private val fileExists: (Path) -> Boolean = { Files.isRegularFile(it) },
) {
    fun resolve(
        logPath: String?,
        projectRoots: List<Path>,
        mappings: List<PluginSettingsState.PathMapping>,
    ): PathResolution {
        if (logPath.isNullOrBlank()) {
            return PathResolution.NotFound(emptyList())
        }

        val direct = safePath(logPath)
        if (direct != null && fileExists(direct)) {
            return PathResolution.Resolved(direct, ResolutionStrategy.Direct)
        }

        resolveMappedPath(logPath, mappings)?.let { mapped ->
            if (fileExists(mapped)) {
                return PathResolution.Resolved(mapped, ResolutionStrategy.Mapping)
            }
        }

        val roots = projectRoots.distinctBy { normalizePath(it.toString()) }
        val markerCandidates = roots.mapNotNull { root ->
            candidateFromProjectRootMarker(logPath, root)?.takeIf(fileExists)
        }.distinctBy { normalizePath(it.toString()) }
        markerCandidates.toResolution(ResolutionStrategy.ProjectRootMarker)?.let { return it }

        val suffixCandidates = longestExistingSuffixCandidates(logPath, roots)
        suffixCandidates.toResolution(ResolutionStrategy.LongestSuffix)?.let { return it }

        return PathResolution.NotFound(buildAttemptedRelativePaths(logPath, roots))
    }

    private fun resolveMappedPath(logPath: String, mappings: List<PluginSettingsState.PathMapping>): Path? {
        val normalizedLog = normalizePath(logPath)
        for (mapping in mappings) {
            val from = normalizePath(mapping.from)
            val to = mapping.to.trim()
            if (from.isBlank() || to.isBlank()) {
                continue
            }
            if (normalizedLog.equals(from, ignoreCase = true)) {
                return safePath(to)
            }
            if (normalizedLog.startsWith("$from/", ignoreCase = true)) {
                val suffix = normalizedLog.removePrefixIgnoringCase("$from/")
                return safePath(joinPathString(to, suffix))
            }
        }
        return null
    }

    private fun candidateFromProjectRootMarker(logPath: String, root: Path): Path? {
        val logSegments = splitSegments(logPath)
        val rootSegments = splitSegments(root.toString())
        if (logSegments.isEmpty() || rootSegments.size < 2) {
            return null
        }

        for (suffixLength in rootSegments.size downTo 2) {
            val rootSuffix = rootSegments.takeLast(suffixLength)
            val start = indexOfSegmentSequence(logSegments, rootSuffix)
            if (start >= 0) {
                val relative = logSegments.drop(start + suffixLength)
                if (relative.isNotEmpty()) {
                    return relative.fold(root) { current, part -> current.resolve(part) }
                }
            }
        }

        return null
    }

    private fun longestExistingSuffixCandidates(logPath: String, roots: List<Path>): List<Path> {
        val logSegments = splitSegments(logPath)
        if (logSegments.isEmpty()) {
            return emptyList()
        }

        for (suffixLength in logSegments.size downTo 1) {
            val suffix = logSegments.takeLast(suffixLength)
            val candidates = roots.map { root ->
                suffix.fold(root) { current, part -> current.resolve(part) }
            }.filter(fileExists).distinctBy { normalizePath(it.toString()) }
            if (candidates.isNotEmpty()) {
                return candidates
            }
        }

        return emptyList()
    }

    private fun buildAttemptedRelativePaths(logPath: String, roots: List<Path>): List<Path> {
        val logSegments = splitSegments(logPath)
        if (logSegments.isEmpty()) {
            return emptyList()
        }
        return logSegments.indices.flatMap { index ->
            val suffix = logSegments.drop(index)
            roots.map { root -> suffix.fold(root) { current, part -> current.resolve(part) } }
        }.distinctBy { normalizePath(it.toString()) }
    }

    private fun List<Path>.toResolution(strategy: ResolutionStrategy): PathResolution? =
        when (size) {
            0 -> null
            1 -> PathResolution.Resolved(first(), strategy)
            else -> PathResolution.Ambiguous(this, strategy)
        }

    private fun safePath(value: String): Path? =
        runCatching {
            Path.of(value.trim().replace('/', java.io.File.separatorChar).replace('\\', java.io.File.separatorChar))
        }.getOrNull()

    private fun splitSegments(path: String): List<String> =
        normalizePath(path)
            .split('/')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "." }

    private fun normalizePath(path: String): String =
        path.trim()
            .replace('\\', '/')
            .replace(Regex("/{2,}"), "/")
            .trimEnd('/')

    private fun joinPathString(base: String, suffix: String): String =
        normalizePath(base) + "/" + suffix.trimStart('/', '\\')

    private fun indexOfSegmentSequence(segments: List<String>, needle: List<String>): Int {
        if (needle.isEmpty() || needle.size > segments.size) {
            return -1
        }
        for (index in 0..(segments.size - needle.size)) {
            val matches = needle.indices.all { offset ->
                segments[index + offset].equals(needle[offset], ignoreCase = true)
            }
            if (matches) {
                return index
            }
        }
        return -1
    }

    private fun String.removePrefixIgnoringCase(prefix: String): String =
        if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this
}

sealed interface PathResolution {
    data class Resolved(val path: Path, val strategy: ResolutionStrategy) : PathResolution
    data class Ambiguous(val candidates: List<Path>, val strategy: ResolutionStrategy) : PathResolution
    data class NotFound(val attemptedPaths: List<Path>) : PathResolution
}

enum class ResolutionStrategy {
    Direct,
    Mapping,
    ProjectRootMarker,
    LongestSuffix,
}
