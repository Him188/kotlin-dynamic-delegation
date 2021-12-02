package compiler

import createInstance
import org.junit.jupiter.api.Test
import runFunction
import testJvmCompile
import kotlin.test.assertEquals

internal class InnerClassesTest : AbstractCompilerTest() {
    @Test
    fun `by lambda`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }

            class WrapperClass {
                fun newTestObject() = TestObject()
                inner class TestObject : TestClass by (dynamicDelegation { getInstanceFromOtherPlaces() })
            }
    
            var called = 0
            fun getInstanceFromOtherPlaces():TestClass  {
                val v = called++
                return object : TestClass {
                    override fun getResult(): Int = v
                }
            }
        """.trimIndent()
    ) {
        classLoader.loadClass("WrapperClass").createInstance().runFunction<Any>("newTestObject").run {
            assertEquals(0, runFunction<Int>("getResult"))
            assertEquals(1, runFunction<Int>("getResult"))
        }
    }
}