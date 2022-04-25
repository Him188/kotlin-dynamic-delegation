package diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals

internal class UnsupportedBodyTest : AbstractDiagnosticsTest() {

    @Test
    fun `non-direct argument`() {
        testJvmCompilation {
            kotlinSource(
                """
                object Main {
                    var count = 1
                    val lambda: () -> String = { "value" + (count++) }
                    fun a(): String = persistent(lambda)
                } 
                """.trimIndent()
            )
            expectFailure {
                withMessages {
                    assertSingleError().run {
                        assertEquals("6, 23", this.location)
                        assertEquals(
                            "'persistent' must directly accept a lambda.",
                            this.message
                        )
                    }
                }
            }
        }

    }
}