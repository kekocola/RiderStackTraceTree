package com.example.rider.stacktrace.tree.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "CSharpStackTraceTreeSettings", storages = [Storage("csharpStackTraceTree.xml")])
class PluginSettingsState : PersistentStateComponent<PluginSettingsState.State> {
    data class PathMapping(
        var from: String = "",
        var to: String = "",
    )

    data class State(
        var autoParse: Boolean = false,
        var pathMappings: MutableList<PathMapping> = mutableListOf(),
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): PluginSettingsState =
            ApplicationManager.getApplication().getService(PluginSettingsState::class.java)
    }
}
