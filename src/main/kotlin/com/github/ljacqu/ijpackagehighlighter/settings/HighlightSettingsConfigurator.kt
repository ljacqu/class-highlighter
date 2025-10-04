package com.github.ljacqu.ijpackagehighlighter.settings

import com.github.ljacqu.ijpackagehighlighter.services.HighlightSettings
import com.github.ljacqu.ijpackagehighlighter.services.HighlightSettingsService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class HighlightSettingsConfigurator(private val project: Project) : Configurable {

    private var settingsComponent: AppSettingsComponent? = null

    override fun getDisplayName() = "Package Highlighting"

    // As specified in https://plugins.jetbrains.com/docs/intellij/settings-guide.html#the-configurable-interface,
    // createComponent() does not need to add any data, as reset() is called right after.
    override fun createComponent(): JComponent {
        if (settingsComponent == null) {
            val component = AppSettingsComponent()
            this.settingsComponent = component
        }
        return settingsComponent!!.mainPanel
    }

    override fun isModified(): Boolean {
        val state = project.getService(HighlightSettings::class.java).state
        val component = settingsComponent ?: return false

        // Check if sections have changed
        if (component.getSections() != state.sectionsToHighlight) {
            return true
        }

        // Check if rules have changed
        val persistedRules = state.rules
        val uiRules = component.getRules()
        if (persistedRules.size != uiRules.size) return true
        return persistedRules.map { AppSettingsComponent.Rule(it) } != uiRules
    }

    override fun apply() {
        val state = project.getService(HighlightSettings::class.java).state
        val newSections = settingsComponent?.getSections() ?: emptySet()
        state.sectionsToHighlight = newSections.toMutableSet()
        val newRules = settingsComponent?.getRules() ?: emptyList()
        state.rules = newRules.map { it.toHighlightRule() }.toMutableList()
        project.getService(HighlightSettingsService::class.java).reload()
    }

    override fun reset() {
        val state = project.getService(HighlightSettings::class.java).state
        settingsComponent?.setSections(state.sectionsToHighlight)
        settingsComponent?.setRules(state.rules)
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
