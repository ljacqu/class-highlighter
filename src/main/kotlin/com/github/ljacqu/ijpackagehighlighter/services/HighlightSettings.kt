package com.github.ljacqu.ijpackagehighlighter.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

const val DEFAULT_COLOR: Int = 0xFFDDC7

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

        var name: String = ""
        var prefix: String = ""
        var rgb: Int = DEFAULT_COLOR

        // needed for XML deserialization
        internal constructor()

        internal constructor(name: String, prefix: String, rgb: Int) {
            this.name = name
            this.prefix = prefix
            this.rgb = rgb
        }
    }

    enum class Section {
        PACKAGE,
        IMPORT,
        JAVADOC,
        METHOD_SIGNATURE,
        CATCH,
        FIELD_TYPE,
        OTHER
    }

    class State {

        // Note: Values must be kept as var here so they're picked up by the serializer properly
        var rules: MutableList<HighlightRule> = ArrayList()
        var sectionsToHighlight: MutableSet<Section> = mutableSetOf()

        init {
            if (rules.isEmpty()) {
                rules.add(HighlightRule("Java util", "java.util.", 0xFFF2CC)) // soft beige
                rules.add(HighlightRule("JDK internal", "jdk.internal.", 0xE2F0D9)) // soft green
                rules.add(HighlightRule("Java lang", "java.lang.", 0xDDEBF7)) // pale blue

                sectionsToHighlight.addAll(Section.entries.toList())
            }
        }
    }
}

