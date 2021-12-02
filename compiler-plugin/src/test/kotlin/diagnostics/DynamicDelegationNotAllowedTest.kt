package diagnostics

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class DynamicDelegationNotAllowedTest : AbstractDiagnosticsTest() {


    @Test
    fun `dynamic delegation not allowed`() {
        testJvmCompilation {
            kotlinSource(
                """
                    interface TestInterface
                    class TestObject : TestInterface by (dynamicDelegation { error("") })
                """.trimIndent()
            )
            useOldBackend()
            expectFailure {
                withMessages {
                    assertSingleError().run {
                        assertEquals("4, 38", this.location)
                        assertEquals("Dynamic delegation is only supported in IR backend.", this.message)
                    }
                }
            }
        }
    }
}