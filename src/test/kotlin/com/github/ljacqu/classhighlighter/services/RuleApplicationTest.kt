package com.github.ljacqu.classhighlighter.services

import com.github.ljacqu.classhighlighter.services.HighlightSettings.Style
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test for [RuleApplication].
 */
class RuleApplicationTest {

    @Test
    fun shouldMatchByWildcard() {
        // given
        val rule = HighlightSettings.HighlightRule("", "*.util.*", "FFEEDD", Style.BACKGROUND)
        val ruleApplication = RuleApplication(rule)

        // when / then
        assertTrue(ruleApplication.matches("java.util.Function"))
        assertFalse(ruleApplication.matches("java.lang.Integer"))
    }

    @Test
    fun shouldMatchByStart() {
        // given
        val rule = HighlightSettings.HighlightRule("", "java.lang.", "EEE9FF", Style.BACKGROUND)
        val ruleApplication = RuleApplication(rule)

        // when / then
        assertTrue(ruleApplication.matches("java.lang.Integer"))
        assertFalse(ruleApplication.matches("java.util.Function"))
        assertFalse(ruleApplication.matches("foo.java.lang.Integer"))
    }
}
