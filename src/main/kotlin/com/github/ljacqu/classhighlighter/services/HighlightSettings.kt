package com.github.ljacqu.classhighlighter.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

const val DEFAULT_COLOR_HEX: String = "FFDDC7"
const val DEFAULT_COLOR: Int = 0xFFDDC7

/**
 * Persisted settings.
 */
@State(
    name = "com.github.ljacqu.classhighlighter.HighlightSettings",
    category = SettingsCategory.PLUGINS,
    storages = [Storage("class-highlighter.xml")]
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
        var rgb: String = DEFAULT_COLOR_HEX
        var style: Style = Style.BACKGROUND

        // needed for XML deserialization
        @Suppress("unused")
        internal constructor()

        internal constructor(name: String, prefix: String, rgb: String, style: Style) {
            this.name = name
            this.prefix = prefix
            this.rgb = rgb
            this.style = style
        }

        companion object {
            val DEFAULT_STYLE: Style = Style.BACKGROUND
        }
    }

    enum class Section {
        PACKAGE,
        IMPORT,
        JAVADOC,
        CONSTRUCTOR,
        METHOD_SIGNATURE,
        CATCH,
        FIELD_TYPE,
        OTHER
    }

    enum class Style(val text: String) {
        BACKGROUND("Background color"),
        TEXT_COLOR("Text color"),
        LINE_UNDERSCORE("Underscored"),
        WAVE_UNDERSCORE("Underwaved"),
        DOTTED_UNDERLINE("Dotted underline"),
        STRIKEOUT("Strikeout"),
        ROUNDED_BOX("Bordered"),
        ;

        companion object {

            fun fromText(text: String): Style =
                entries.firstOrNull { e -> e.text == text } ?: HighlightRule.DEFAULT_STYLE

        }
    }

    class State {

        // Note: Values must be kept as var here so they're picked up by the serializer properly
        var rules: MutableList<HighlightRule> = ArrayList()
        var sectionsToHighlight: MutableSet<Section> = mutableSetOf()

        init {
            if (rules.isEmpty()) {
                val style = HighlightRule.DEFAULT_STYLE
                rules.add(HighlightRule("Java util", "java.util.", "FFF2CC", style)) // soft beige
                rules.add(HighlightRule("JDK internal", "jdk.internal.", "E2F0D9", style)) // soft green
                rules.add(HighlightRule("Java lang", "java.lang.", "DDEBF7", style)) // pale blue

                sectionsToHighlight.addAll(Section.entries.toList())
            }
        }
    }
}
