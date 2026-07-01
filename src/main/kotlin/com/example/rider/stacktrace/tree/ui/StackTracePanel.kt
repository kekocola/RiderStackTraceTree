package com.example.rider.stacktrace.tree.ui

import com.example.rider.stacktrace.tree.model.ExceptionEntry
import com.example.rider.stacktrace.tree.model.FrameEntry
import com.example.rider.stacktrace.tree.model.ResolveStatus
import com.example.rider.stacktrace.tree.model.StackFrame
import com.example.rider.stacktrace.tree.model.StackTraceEntry
import com.example.rider.stacktrace.tree.model.TextEntry
import com.example.rider.stacktrace.tree.navigation.StackFrameNavigator
import com.example.rider.stacktrace.tree.parser.CSharpStackTraceParser
import com.example.rider.stacktrace.tree.resolution.PathResolution
import com.example.rider.stacktrace.tree.resolution.SourcePathResolver
import com.example.rider.stacktrace.tree.settings.PluginSettingsState
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.nio.file.Path
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.event.DocumentEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import kotlin.io.path.Path

class StackTracePanel(private val project: Project) : JPanel(BorderLayout()) {
    private val parser = CSharpStackTraceParser()
    private val resolver = SourcePathResolver()
    private val navigator = StackFrameNavigator(project)
    private val settings = PluginSettingsState.getInstance()

    private val input = JBTextArea().apply {
        lineWrap = false
        emptyText.text = "Paste a C#/.NET stack trace here"
    }
    private val rootNode = DefaultMutableTreeNode("Stack Trace")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = JTree(treeModel).apply {
        isRootVisible = true
        showsRootHandles = true
    }
    private val status = JLabel("Paste a stack trace and click Parse.")
    private val autoParseTimer = Timer(350) { parseAndRender() }.apply {
        isRepeats = false
    }

    init {
        add(toolbar(), BorderLayout.NORTH)
        add(splitPane(), BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)

        input.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (settings.state.autoParse) {
                    autoParseTimer.restart()
                }
            }
        })

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount >= 2 || SwingUtilities.isLeftMouseButton(e)) {
                    navigateSelectedFrame()
                }
            }
        })
    }

    private fun toolbar(): JPanel =
        JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("Parse").apply { addActionListener { parseAndRender() } })
            add(JButton("Clear").apply {
                addActionListener {
                    input.text = ""
                    rootNode.removeAllChildren()
                    treeModel.reload()
                    status.text = "Cleared."
                }
            })
            add(JButton("Copy Selected").apply { addActionListener { copySelectedNode() } })
            add(JButton("Settings").apply { addActionListener { showSettings() } })
        }

    private fun splitPane(): JSplitPane =
        JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            JBScrollPane(input),
            JBScrollPane(tree),
        ).apply {
            resizeWeight = 0.45
        }

    private fun parseAndRender() {
        val entries = parser.parse(input.text)
        val roots = projectRoots()
        val mappings = settings.state.pathMappings

        rootNode.removeAllChildren()
        rootNode.userObject = "Stack Trace (${entries.count { it is FrameEntry }} frames)"

        var currentExceptionNode: DefaultMutableTreeNode? = null
        entries.forEach { entry ->
            when (entry) {
                is ExceptionEntry -> {
                    val exceptionNode = DefaultMutableTreeNode(entry.title)
                    rootNode.add(exceptionNode)
                    currentExceptionNode = exceptionNode
                }

                is FrameEntry -> {
                    val resolved = resolveFrame(entry.frame, roots, mappings)
                    val frameNode = DefaultMutableTreeNode(FrameNodePayload(resolved))
                    (currentExceptionNode ?: rootNode).add(frameNode)
                }

                is TextEntry -> {
                    val textNode = DefaultMutableTreeNode(entry.rawLine.trim())
                    (currentExceptionNode ?: rootNode).add(textNode)
                }
            }
        }

        treeModel.reload()
        expandFirstLevels()
        status.text = "Parsed ${entries.count { it is FrameEntry }} frames. Roots: ${roots.size}."
    }

    private fun resolveFrame(
        frame: StackFrame,
        roots: List<Path>,
        mappings: List<PluginSettingsState.PathMapping>,
    ): ResolvedFrame {
        return when (val resolution = resolver.resolve(frame.absolutePathFromLog, roots, mappings)) {
            is PathResolution.Resolved -> ResolvedFrame(frame.copy(resolvedPath = resolution.path.toString(), resolveStatus = ResolveStatus.Resolved), listOf(resolution.path), emptyList())
            is PathResolution.Ambiguous -> ResolvedFrame(frame.copy(resolveStatus = ResolveStatus.Ambiguous), resolution.candidates, emptyList())
            is PathResolution.NotFound -> ResolvedFrame(frame.copy(resolveStatus = ResolveStatus.NotFound), emptyList(), resolution.attemptedPaths)
        }
    }

    private fun navigateSelectedFrame() {
        val payload = (tree.lastSelectedPathComponent as? DefaultMutableTreeNode)?.userObject as? FrameNodePayload ?: return
        val resolvedFrame = payload.resolvedFrame
        when (resolvedFrame.frame.resolveStatus) {
            ResolveStatus.Resolved -> {
                val path = resolvedFrame.candidates.firstOrNull() ?: resolvedFrame.frame.resolvedPath?.let { Path(it) } ?: return
                navigator.navigate(path, resolvedFrame.frame.lineNumber)
            }

            ResolveStatus.Ambiguous -> chooseAmbiguousCandidate(resolvedFrame)
            ResolveStatus.NotFound, ResolveStatus.NotResolved -> showNotFound(resolvedFrame)
        }
    }

    private fun chooseAmbiguousCandidate(resolvedFrame: ResolvedFrame) {
        val options = resolvedFrame.candidates.map { it.toString() }.toTypedArray()
        val choice = JOptionPane.showInputDialog(
            this,
            "Multiple source files matched this stack frame.",
            "Choose Source File",
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options.firstOrNull(),
        ) as? String
        if (!choice.isNullOrBlank()) {
            navigator.navigate(Path(choice), resolvedFrame.frame.lineNumber)
        }
    }

    private fun showNotFound(resolvedFrame: ResolvedFrame) {
        val attempts = resolvedFrame.attemptedPaths.take(12).joinToString("\n") { it.toString() }
        Messages.showInfoMessage(
            project,
            "Could not locate ${resolvedFrame.frame.absolutePathFromLog.orEmpty()}.\n\nAttempted paths:\n$attempts",
            "Source File Not Found",
        )
    }

    private fun showSettings() {
        val dialog = StackTraceSettingsDialog(project, settings)
        if (dialog.showAndGet()) {
            parseAndRender()
        }
    }

    private fun copySelectedNode() {
        val text = tree.selectionPaths
            ?.joinToString("\n") { path -> path.lastPathComponent.toString() }
            .orEmpty()
        if (text.isNotBlank()) {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            status.text = "Copied selected node."
        }
    }

    private fun projectRoots(): List<Path> {
        val contentRoots = ProjectRootManager.getInstance(project).contentRoots
            .mapNotNull { runCatching { Path(it.path) }.getOrNull() }
        val basePath = project.basePath?.let { Path(it) }
        return (contentRoots + listOfNotNull(basePath)).distinctBy { it.toString().lowercase() }
    }

    private fun expandFirstLevels() {
        for (row in 0 until tree.rowCount.coerceAtMost(20)) {
            tree.expandRow(row)
        }
    }

    private data class ResolvedFrame(
        val frame: StackFrame,
        val candidates: List<Path>,
        val attemptedPaths: List<Path>,
    )

    private data class FrameNodePayload(val resolvedFrame: ResolvedFrame) {
        override fun toString(): String {
            val frame = resolvedFrame.frame
            val file = frame.fileName ?: frame.absolutePathFromLog.orEmpty()
            val line = frame.lineNumber?.let { ":$it" }.orEmpty()
            val suffix = when (frame.resolveStatus) {
                ResolveStatus.Resolved -> ""
                ResolveStatus.Ambiguous -> " [multiple matches]"
                ResolveStatus.NotFound -> " [not found]"
                ResolveStatus.NotResolved -> " [not resolved]"
            }
            return "${frame.method}  $file$line$suffix"
        }
    }
}
