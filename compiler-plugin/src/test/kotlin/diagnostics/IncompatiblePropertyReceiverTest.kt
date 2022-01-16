package diagnostics

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class IncompatiblePropertyReceiverTest : AbstractDiagnosticsTest() {

    @Test
    fun `dynamic delegation not allowed`() {
        testJvmCompilation {
            kotlinSource(
                """
            interface TestClass {
                val result: Int
            }
            
            class IncompatibleClass {
                val refined: TestClass get() = error("")
            }
            
            class CombinedMessage : TestClass by dynamicDelegation(IncompatibleClass::refined)
                """.trimIndent()
            )
            expectFailure {
                withMessages {
                    assertSingleError().run {
                        assertEquals("11, 56", this.location)
                        assertEquals(
                            "Property receiver is not compatible with type 'CombinedMessage'",
                            this.message
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `properties with dispatch receiver parameter from child type`() = testJvmCompilation {
        kotlinSource(
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
            
            open class CombinedMessage : Message(), TestClass by dynamicDelegation(Child::refined)
            
            class Child : CombinedMessage()
        """.trimIndent()
        )
        expectFailure {
            withMessages {
                assertSingleError().run {
                    assertEquals("17, 72", this.location)
                    assertEquals(
                        "Property receiver is not compatible with type 'CombinedMessage'",
                        this.message
                    )
                }
            }
        }
    }
}