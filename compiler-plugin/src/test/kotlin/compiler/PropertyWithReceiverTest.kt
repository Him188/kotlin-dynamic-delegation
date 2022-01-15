package compiler

import createInstance
import org.junit.jupiter.api.Test
import runFunction
import kotlin.test.assertEquals

internal class PropertyWithReceiverTest : AbstractCompilerTest() {
    @Test
    fun `properties with dispatch receiver parameter`() = testJvmCompile(
        """
            interface TestClass {
                val result: Int
            }
            
            class CombinedMessage : TestClass by dynamicDelegation(CombinedMessage::refined) {
                val lazy: Lazy<TestClass> = lazy {
                    object : TestClass {
                        override val result: Int get() = 1
                    }
                }
                val lazyInitialized get() = lazy.isInitialized()
                val refined: TestClass by lazy
            }
        """.trimIndent()
    ) {
        classLoader.loadClass("CombinedMessage").createInstance().run {
            assertEquals(false, runFunction<Boolean>("getLazyInitialized"))
            assertEquals(1, runFunction<Int>("getResult"))
            assertEquals(true, runFunction<Boolean>("getLazyInitialized"))
        }
    }

    @Test
    fun `properties with dispatch receiver parameter from supertype`() = testJvmCompile(
        """
            interface TestClass {
                val result: Int
            }
            
            abstract class Message {
                val lazy: Lazy<TestClass> = lazy {
                    object : TestClass {
                        override val result: Int get() = 1
                    }
                }
                val lazyInitialized get() = lazy.isInitialized()
                val refined: TestClass by lazy
            }
            
            class CombinedMessage : Message(), TestClass by dynamicDelegation(Message::refined)
        """.trimIndent()
    ) {
        classLoader.loadClass("CombinedMessage").createInstance().run {
            assertEquals(false, runFunction<Boolean>("getLazyInitialized"))
            assertEquals(1, runFunction<Int>("getResult"))
            assertEquals(true, runFunction<Boolean>("getLazyInitialized"))
        }
    }
}