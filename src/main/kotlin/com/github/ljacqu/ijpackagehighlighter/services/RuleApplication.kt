package com.github.ljacqu.ijpackagehighlighter.services

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

    fun createTextAttributes(): TextAttributes {
        val bg = Color(rule.rgb)
        return TextAttributes(null, bg, null, null, Font.PLAIN)
    }

    fun getName(): String = rule.name

    private fun createRegexFilter(wildcardPattern: String): (String) -> Boolean {
        val parts = wildcardPattern.split("*").map { Regex.escape(it) }
        val regexPattern = parts.joinToString(".*")
        val regex = Regex("^$regexPattern$")
        return { input -> regex.matches(input) }
    }
}
