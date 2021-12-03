package diagnostics

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class DynamicDelegationNotAllowedTest : AbstractDiagnosticsTest() {

    @Test
    fun `dynamic delegation not allowed`() {
        testJvmCompilation {
            kotlinSource(
                """
                    interface I
                    class C : I by (dynamicDelegation { TODO() })
                    
                    fun m() {
                        dynamicDelegation { object : I {} }
                    }
                """.trimIndent()
            )
            expectFailure {
                withMessages {
                    assertSingleError().run {
                        assertEquals("7, 5", this.location)
                        assertEquals(
                            "Dynamic delegation is not allowed here. It should only be used in class delegations.",
                            this.message
                        )
                    }
                }
            }
        }
    }
}