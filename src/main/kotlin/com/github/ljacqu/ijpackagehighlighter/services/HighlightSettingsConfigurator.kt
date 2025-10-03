package com.github.ljacqu.ijpackagehighlighter.services

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import javax.swing.JComponent

class HighlightSettingsConfigurator(private val project: Project) : Configurable {

    private var component: AppSettingsComponent? = null

    override fun getDisplayName(): String = "Package Highlighting"

    override fun createComponent(): JComponent? {
        val state = project.getService(HighlightSettings::class.java).state
        val component = AppSettingsComponent(state)
        this.component = component
        return component.panel
    }

    override fun isModified(): Boolean {
        return false // todo
    }

    override fun apply() {
        val state = project.getService(HighlightSettings::class.java).state


        // todo
    }


}