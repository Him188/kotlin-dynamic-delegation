package compiler

import createInstance
import org.junit.jupiter.api.Test
import runFunction

internal class LocalClassesTest : AbstractCompilerTest() {
    @Test
    fun `by lambda`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }
            object FileClass {
            
                fun fn() {
                    class TestObject : TestClass by (dynamicDelegation { getInstanceFromOtherPlaces() })
            
                    TestObject().run { 
                
                        assertEquals(0, getResult())
                        assertEquals(1, getResult())
                    }
                }
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
        classLoader.loadClass("FileClass").createInstance().runFunction<Unit>("fn")
    }

    @Test
    fun `by local fun`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }
            object FileClass {
            
                fun fn() {
                    var called = 0
                    fun getInstanceFromOtherPlaces():TestClass  {
                        val v = called++
                        return object : TestClass {
                            override fun getResult(): Int = v
                        }
                    }
                    class TestObject : TestClass by (dynamicDelegation { getInstanceFromOtherPlaces() })
            
                    TestObject().run { 
                
                        assertEquals(0, getResult())
                        assertEquals(1, getResult())
                    }
                }
            }
        """.trimIndent()
    ) {
        classLoader.loadClass("FileClass").createInstance().runFunction<Unit>("fn")
    }
}