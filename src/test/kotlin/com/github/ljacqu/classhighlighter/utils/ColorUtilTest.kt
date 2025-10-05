package com.github.ljacqu.classhighlighter.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test for [ColorUtil].
 */
class ColorUtilTest {

    @Test
    fun shouldConvertToHexString() {
        // given
        val value = 0xF9D7B5

        // when
        val result = ColorUtil.intToHexString(value)

        // then
        assertEquals("F9D7B5", result)
    }

    @Test
    fun shouldConvertToHexWithSixChars() {
        // given / when / then
        assertEquals("000000", ColorUtil.intToHexString(0x000000))
        assertEquals("0A0501", ColorUtil.intToHexString(0x0A0501))
        assertEquals("BEEF00", ColorUtil.intToHexString(0x18BEEF00))
    }

    @Test
    fun shouldConvertToIntValue() {
        // given
        val value = "F9D7B5"

        // when
        val result = ColorUtil.hexStringToInt(value)

        // then
        assertEquals(0xF9D7B5, result)
    }

    @Test
    fun shouldConvertToIntValue2() {
        // given / when / then
        assertEquals(0x000000, ColorUtil.hexStringToInt("000000"))
        assertEquals(0x0A0501, ColorUtil.hexStringToInt("0A0501"))
        assertEquals(0xBEEF00, ColorUtil.hexStringToInt("18BEEF00"))
    }
}
