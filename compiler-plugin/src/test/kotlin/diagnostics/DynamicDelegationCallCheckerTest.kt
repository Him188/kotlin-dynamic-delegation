package diagnostics

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class DynamicDelegationCallCheckerTest : AbstractDiagnosticsTest() {

    @Test
    fun `dynamic delegation in only available with IR`() {
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