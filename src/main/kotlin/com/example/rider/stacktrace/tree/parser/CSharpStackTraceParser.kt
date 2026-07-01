package com.example.rider.stacktrace.tree.parser

import com.example.rider.stacktrace.tree.model.ExceptionEntry
import com.example.rider.stacktrace.tree.model.FrameEntry
import com.example.rider.stacktrace.tree.model.StackFrame
import com.example.rider.stacktrace.tree.model.StackTraceEntry
import com.example.rider.stacktrace.tree.model.TextEntry

class CSharpStackTraceParser {
    private val framePattern = Regex("""^\s*at\s+(.+?)\s+in\s+(.+):line\s+(\d+)\s*$""")
    private val exceptionPattern = Regex("""^\s*(--->\s*)?([A-Za-z_][\w.`+]*\.)*[A-Za-z_][\w.`+]*(Exception|Error)\b.*$""")

    fun parse(text: String): List<StackTraceEntry> {
        if (text.isBlank()) {
            return emptyList()
        }

        var currentInnerLevel = 0
        return text.lineSequence()
            .filter { it.isNotBlank() }
            .map { line ->
                val trimmed = line.trim()
                val frameMatch = framePattern.matchEntire(line)
                when {
                    frameMatch != null -> {
                        val method = frameMatch.groupValues[1].trim()
                        val path = frameMatch.groupValues[2].trim()
                        val lineNumber = frameMatch.groupValues[3].toIntOrNull()
                        FrameEntry(
                            rawLine = line,
                            frame = StackFrame(
                                rawLine = line,
                                method = method,
                                absolutePathFromLog = path,
                                fileName = path.substringAfterLast('\\').substringAfterLast('/'),
                                lineNumber = lineNumber,
                                innerExceptionLevel = currentInnerLevel,
                            )
                        )
                    }

                    exceptionPattern.matches(trimmed) -> {
                        if (trimmed.startsWith("--->")) {
                            currentInnerLevel += 1
                        }
                        ExceptionEntry(
                            rawLine = line,
                            title = trimmed.removePrefix("--->").trim(),
                            innerExceptionLevel = currentInnerLevel,
                        )
                    }

                    trimmed.startsWith("--- End of inner exception stack trace ---") -> {
                        currentInnerLevel = (currentInnerLevel - 1).coerceAtLeast(0)
                        TextEntry(line)
                    }

                    else -> TextEntry(line)
                }
            }
            .toList()
    }
}
