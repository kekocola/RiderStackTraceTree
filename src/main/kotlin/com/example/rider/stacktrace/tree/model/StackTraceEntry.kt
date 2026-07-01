package com.example.rider.stacktrace.tree.model

sealed interface StackTraceEntry {
    val rawLine: String
}

data class ExceptionEntry(
    override val rawLine: String,
    val title: String,
    val innerExceptionLevel: Int,
) : StackTraceEntry

data class FrameEntry(
    override val rawLine: String,
    val frame: StackFrame,
) : StackTraceEntry

data class TextEntry(
    override val rawLine: String,
) : StackTraceEntry
