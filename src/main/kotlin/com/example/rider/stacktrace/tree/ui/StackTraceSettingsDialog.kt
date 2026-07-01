package com.example.rider.stacktrace.tree.ui

import com.example.rider.stacktrace.tree.settings.PluginSettingsState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class StackTraceSettingsDialog(
    project: Project,
    private val settings: PluginSettingsState,
) : DialogWrapper(project) {
    private val autoParse = JBCheckBox("Parse automatically after paste/edit", settings.state.autoParse)
    private val mappings = JBTextArea().apply {
        lineWrap = false
        text = settings.state.pathMappings.joinToString("\n") { "${it.from} => ${it.to}" }
        emptyText.text = "D:\\Server2\\pokeworld_dev\\Server\\GameServer => D:\\Server2\\pokeworld\\Server\\GameServer"
    }

    init {
        title = "Stack Trace Tree Settings"
        init()
    }

    override fun createCenterPanel(): JComponent =
        JPanel(BorderLayout(8, 8)).apply {
            preferredSize = Dimension(720, 320)
            add(autoParse, BorderLayout.NORTH)
            add(
                JPanel(BorderLayout(4, 4)).apply {
                    add(JLabel("Path mappings, one per line: From => To"), BorderLayout.NORTH)
                    add(JBScrollPane(mappings), BorderLayout.CENTER)
                },
                BorderLayout.CENTER,
            )
        }

    override fun doOKAction() {
        settings.state.autoParse = autoParse.isSelected
        settings.state.pathMappings = mappings.text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("=>", limit = 2)
                if (parts.size == 2) {
                    PluginSettingsState.PathMapping(parts[0].trim(), parts[1].trim())
                } else {
                    null
                }
            }
            .toMutableList()
        super.doOKAction()
    }
}
