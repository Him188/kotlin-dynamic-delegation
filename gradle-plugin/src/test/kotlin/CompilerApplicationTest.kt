package me.him188.kotlin.dynamic.delegation

import org.junit.jupiter.api.Test

class CompilerApplicationTest : AbstractCompilerPluginTest() {

    @Test
    fun `can apply plugin`() {
        runGradleBuild(tempDir, "build")
    }
}