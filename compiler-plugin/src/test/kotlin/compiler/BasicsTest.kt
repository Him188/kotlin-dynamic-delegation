@file:Suppress("RemoveRedundantBackticks", "RedundantSuspendModifier", "MainFunctionReturnUnit")

package compiler

import org.junit.jupiter.api.Test
import runFunction
import testJvmCompile
import kotlin.test.assertEquals

internal class BasicsTest : AbstractCompilerTest() {
    @Test
    fun `generate single for simple object`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }

            object TestObject : TestClass by (dynamicDelegation { getInstanceFromOtherPlaces() })
    
            var called = 0
            fun getInstanceFromOtherPlaces() = object : TestClass {
                override fun getResult(): Int = called++
            }
        """
    ) {
        classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
            assertEquals(0, runFunction<Int>("getResult"))
            assertEquals(1, runFunction<Int>("getResult"))
        }
    }
}
