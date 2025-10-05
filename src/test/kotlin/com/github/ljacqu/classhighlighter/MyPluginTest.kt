package com.github.ljacqu.classhighlighter

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    // Basically a gitkeep for now :)

    override fun getTestDataPath() = "src/test/testData/rename"

    @Test
    fun test() {
        // Prevent warning that there are no tests
        assertEquals(1, 1)
    }
}
