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
                        assertEquals("11, 38", this.location)
                        assertEquals(
                            "Property receiver is not compatible with type 'CombinedMessage'",
                            this.message
                        )
                    }
                }
            }
        }
    }
}