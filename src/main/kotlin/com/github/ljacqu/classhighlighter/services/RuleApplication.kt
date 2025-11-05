package com.github.ljacqu.classhighlighter.services

import com.github.ljacqu.classhighlighter.utils.ColorUtil
import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font

/**
 * Contains a highlight rule and allows to process it.
 */
class RuleApplication(private val rule: HighlightSettings.HighlightRule) {

    private val filter: Function1<String, Boolean>

    init {
        filter = if (!rule.prefix.contains("*")) {
            qualifiedName -> qualifiedName.startsWith(rule.prefix)
        } else {
            createRegexFilter(rule.prefix)
        }
    }

    fun matches(qualifiedName: String) = filter(qualifiedName)

    fun getName(): String = rule.name

    fun newAnnotationBuilder(holder: AnnotationHolder, name: String?): AnnotationBuilder {
        val holder = if (name.isNullOrEmpty()) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
        } else {
            holder.newAnnotation(HighlightSeverity.INFORMATION, name)
        }
        holder.enforcedTextAttributes(createTextAttributes())
        return holder
    }

    private fun createTextAttributes(): TextAttributes {
        val color = Color(ColorUtil.hexStringToInt(rule.rgb))

        return when (rule.style) {
            HighlightSettings.Style.BACKGROUND -> TextAttributes(null, color, null, null, Font.PLAIN)
            HighlightSettings.Style.TEXT_COLOR -> TextAttributes(color, null, null, null, Font.PLAIN)
            HighlightSettings.Style.LINE_UNDERSCORE -> TextAttributes(null, null, color, EffectType.BOLD_LINE_UNDERSCORE, Font.PLAIN)
            HighlightSettings.Style.WAVE_UNDERSCORE -> TextAttributes(null, null, color, EffectType.WAVE_UNDERSCORE, Font.PLAIN)
            HighlightSettings.Style.DOTTED_UNDERLINE -> TextAttributes(null, null, color, EffectType.BOLD_DOTTED_LINE, Font.PLAIN)
            HighlightSettings.Style.STRIKEOUT -> TextAttributes(null, null, color, EffectType.STRIKEOUT, Font.PLAIN)
            HighlightSettings.Style.ROUNDED_BOX -> TextAttributes(null, null, color, EffectType.ROUNDED_BOX, Font.PLAIN)
        }
    }

    private fun createRegexFilter(wildcardPattern: String): (String) -> Boolean {
        val parts = wildcardPattern.split("*").map { Regex.escape(it) }
        val regexPattern = parts.joinToString(".*")
        val regex = Regex("^$regexPattern$")
        return { input -> regex.matches(input) }
    }
}
