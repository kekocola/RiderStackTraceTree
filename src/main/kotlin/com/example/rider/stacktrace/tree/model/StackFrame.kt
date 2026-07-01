package com.example.rider.stacktrace.tree.model

data class StackFrame(
    val rawLine: String,
    val method: String,
    val absolutePathFromLog: String?,
    val fileName: String?,
    val lineNumber: Int?,
    val exceptionHeader: String? = null,
    val innerExceptionLevel: Int = 0,
    val resolvedPath: String? = null,
    val resolveStatus: ResolveStatus = ResolveStatus.NotResolved,
)

enum class ResolveStatus {
    NotResolved,
    Resolved,
    NotFound,
    Ambiguous,
}
