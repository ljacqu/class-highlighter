package com.github.ljacqu.ijpackagehighlighter.services

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import javax.swing.JComponent

class HighlightSettingsConfigurator(private val project: Project) : Configurable {

    private var component: AppSettingsComponent? = null

    override fun getDisplayName(): String = "Package Highlighting"

    override fun createComponent(): JComponent? {
        val component = AppSettingsComponent()
        this.component = component
        return component.panel
    }

    override fun isModified(): Boolean {
        return false // todo
    }

    override fun apply() {
        val s = project.getService(HighlightSettings::class.java)
        // todo
    }


}