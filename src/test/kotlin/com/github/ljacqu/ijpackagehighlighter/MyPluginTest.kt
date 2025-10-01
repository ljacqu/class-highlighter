package com.github.ljacqu.ijpackagehighlighter

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    // Basically a gitkeep for now :)

    override fun getTestDataPath() = "src/test/testData/rename"
}
