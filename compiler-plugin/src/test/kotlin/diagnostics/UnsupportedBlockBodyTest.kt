package diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals

internal class UnsupportedBlockBodyTest : AbstractDiagnosticsTest() {

    @Test
    fun `local var`() {
        testJvmCompilation {
            kotlinSource(
                """
                object Main {
                    var count = 1
                    fun a(): String {
                        val x = 1
                        return persistent { "value" + x + (count++) }
                    }
                } 
                """.trimIndent()
            )
            expectFailure {
                withMessages {
                    assertSingleError().run {
                        assertEquals("7, 16", this.location)
                        assertEquals(
                            "'persistent' is not allowed in block body.",
                            this.message
                        )
                    }
                }
            }
        }

    }
}