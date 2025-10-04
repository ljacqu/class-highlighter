package com.github.ljacqu.ijpackagehighlighter.services

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class HighlightSettingsConfigurator(private val project: Project) : Configurable {

    private var settingsComponent: AppSettingsComponent? = null

    override fun getDisplayName(): String = "Package Highlighting"

    override fun createComponent(): JComponent {
        if (settingsComponent == null) {
            val state = project.getService(HighlightSettings::class.java).state
            val component = AppSettingsComponent(state)
            this.settingsComponent = component
        }
        return settingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val state = project.getService(HighlightSettings::class.java).state
        val persisted = state.groups
        val uiRules = settingsComponent?.getRules() ?: return false
        if (persisted.size != uiRules.size) return true
        return persisted.zip(uiRules).any { (p, u) -> p.prefix != u.prefix || p.rgb != u.rgb }
    }

    override fun apply() {
        val state = project.getService(HighlightSettings::class.java).state
        val newGroups = settingsComponent?.getRules() ?: emptyList()
        state.groups = newGroups.toMutableList()
    }

    override fun reset() {
        val state = project.getService(HighlightSettings::class.java).state
        settingsComponent?.setRules(state.groups)
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}