package com.github.ljacqu.ijpackagehighlighter.services

import com.github.ljacqu.ijpackagehighlighter.utils.ColorUtil
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test for [HighlightSettings].
 */
class HighlightSettingsTest {

    @Test
    fun shouldHaveDefaultColorValuesInSync() {
        // given / when / then
        assertEquals(DEFAULT_COLOR_HEX, ColorUtil.intToHexString(DEFAULT_COLOR))
    }

}
