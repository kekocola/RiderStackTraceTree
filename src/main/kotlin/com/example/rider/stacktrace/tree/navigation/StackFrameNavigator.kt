package com.example.rider.stacktrace.tree.navigation

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Path

class StackFrameNavigator(private val project: Project) {
    fun navigate(path: Path, lineNumber: Int?) {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path.toString().replace('\\', '/')) ?: return
        OpenFileDescriptor(project, virtualFile, ((lineNumber ?: 1) - 1).coerceAtLeast(0), 0).navigate(true)
    }
}
