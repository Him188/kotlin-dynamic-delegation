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
            repeat(3) { assertEquals("constant", runFunction<String>("a")) }
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

    @Test
    fun `dispatch receiver mapping`() = testJvmCompile(
        """
            @file:JvmName("MainKt")
            class MessageChain(
                private val values: List<String>
            ) {
                override fun toString(): String = persistent { values.joinToString() }
            }

            fun a() = MessageChain(listOf("1", "2")).toString()
        """.trimIndent()
    ) {
        classLoader.loadClass("MainKt").run {
            assertEquals("1, 2", runStaticFunction<String>("a"))
        }
    }

    @Test
    fun `type arguments`() = testJvmCompile(
        """
            @file:JvmName("MainKt")
            class MessageChain<T>(
                private val values: List<T>
            ) {
                fun get(): T = persistent { values.first() }
            }

            fun a() = MessageChain(listOf("1", "2")).get()
        """.trimIndent()
    ) {
        classLoader.loadClass("MainKt").run {
            assertEquals("1", runStaticFunction<Any>("a"))
        }
    }

    @Test
    fun `type arguments 2`() = testJvmCompile(
        """
            @file:JvmName("MainKt")
            data class MessageChain<T>(
                private val values: List<T>
            ) {
                fun get(): MessageChain<T> = persistent { this }
            }

            fun a() = MessageChain(listOf("1", "2")).toString()
        """.trimIndent()
    ) {
        classLoader.loadClass("MainKt").run {
            assertEquals("MessageChain(values=[1, 2])", runStaticFunction<Any>("a"))
        }
    }
}