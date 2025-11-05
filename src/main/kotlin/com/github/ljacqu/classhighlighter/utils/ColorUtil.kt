package com.github.ljacqu.classhighlighter.utils

/**
 * Color utilities.
 */
object ColorUtil {

    private val hexFormat = java.util.HexFormat.of().withUpperCase()

    /**
     * Converts the given int to a 6-digit uppercase hex string, e.g. "F0CCA9" for 0xF0CCA9.
     */
    fun intToHexString(rgb: Int): String = hexFormat.toHexDigits(rgb).substring(2)

    /**
     * Converts the given hex string to its integer value, only considering the 6 leftmost bytes.
     */
    fun hexStringToInt(hex: String) = java.util.HexFormat.fromHexDigits(hex) and 0xFFFFFF

}
