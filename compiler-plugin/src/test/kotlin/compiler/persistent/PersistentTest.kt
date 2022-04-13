package compiler.persistent

import compiler.AbstractCompilerTest
import createInstance
import org.junit.jupiter.api.Test
import runFunction
import runStaticFunction
import kotlin.test.assertEquals

internal class PersistentTest : AbstractCompilerTest() {

    @Test
    fun `member function, constant value`() = testJvmCompile(
        """
            object Main {
                fun a(): String = persistent { "constant" }
            } 
        """.trimIndent()
    ) {
        classLoader.loadClass("Main").createInstance().run {
            repeat(3) { assertEquals("value1", runFunction<String>("a")) }
        }
    }

    @Test
    fun `member function, inits only once`() = testJvmCompile(
        """
            object Main {
                var count = 1
                fun a(): String = persistent { "value" + (count++) }
            } 
        """.trimIndent()
    ) {
        classLoader.loadClass("Main").createInstance().run {
            repeat(3) { assertEquals("value1", runFunction<String>("a")) }
        }
    }

    @Test
    fun `top-level function, constant value`() = testJvmCompile(
        """
            @file:JvmName("MainKt")
            fun a(): String = persistent { "value1" }
        """.trimIndent()
    ) {
        classLoader.loadClass("MainKt").run {
            repeat(3) { assertEquals("value1", runStaticFunction<String>("a")) }
        }
    }

    @Test
    fun `top-level function, inits only once`() = testJvmCompile(
        """
            @file:JvmName("MainKt")
            var count = 1
            fun a(): String = persistent { "value" + (count++) }
        """.trimIndent()
    ) {
        classLoader.loadClass("MainKt").run {
            repeat(3) { assertEquals("value1", runStaticFunction<String>("a")) }
        }
    }
}