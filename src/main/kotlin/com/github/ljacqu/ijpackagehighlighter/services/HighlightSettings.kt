package com.github.ljacqu.ijpackagehighlighter.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.ui.EditableModel
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.ljacqu.ijpackagehighlighter.HighlightSettings",
    category = SettingsCategory.PLUGINS,
    storages = [Storage("package-highlighter.xml")]
)
class HighlightSettings : PersistentStateComponent<HighlightSettings.State> {

    private val state: State = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    class HighlightRule {
        var prefix: String = ""
        var rgb: Int = 0xFFFF00 // default yellow background

        // needed for XML deserialization
        internal constructor()

        internal constructor(prefix: String, rgb: Int) {
            this.prefix = prefix
            this.rgb = rgb
        }
    }

    class State {
        val groups: MutableList<HighlightRule> = ArrayList()

        init {
            if (groups.isEmpty()) {
                groups.add(HighlightRule("java.util.", 0xFFF2CC)) // soft beige
                groups.add(HighlightRule("jdk.internal.", 0xE2F0D9)) // soft green
                groups.add(HighlightRule("java.lang.", 0xDDEBF7)) // pale blue
            }
        }
    }
}

