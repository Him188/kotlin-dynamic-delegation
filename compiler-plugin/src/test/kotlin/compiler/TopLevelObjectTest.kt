@file:Suppress("RemoveRedundantBackticks", "RedundantSuspendModifier", "MainFunctionReturnUnit")

package compiler

import org.junit.jupiter.api.Test
import runFunction
import kotlin.test.assertEquals

internal class TopLevelObjectTest : AbstractCompilerTest() {
    @Test
    fun `by lambda`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }

            object TestObject : TestClass by (dynamicDelegation { getInstanceFromOtherPlaces() })
    
            var called = 0
            fun getInstanceFromOtherPlaces():TestClass  {
                val v = called++
                return object : TestClass {
                    override fun getResult(): Int = v
                }
            }
        """
    ) {
        classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
            assertEquals(0, runFunction<Int>("getResult"))
            assertEquals(1, runFunction<Int>("getResult"))
        }
    }

    @Test
    fun `by lambda with derived type`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }
            interface TestClass2 : TestClass

            object TestObject : TestClass by (dynamicDelegation { getInstanceFromOtherPlaces() })
    
            var called = 0
            fun getInstanceFromOtherPlaces():TestClass2  {
                val v = called++
                return object : TestClass2 {
                    override fun getResult(): Int = v
                }
            }
        """
    ) {
        classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
            assertEquals(0, runFunction<Int>("getResult"))
            assertEquals(1, runFunction<Int>("getResult"))
        }
    }

    @Test
    fun `by function reference`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }

            object TestObject : TestClass by (dynamicDelegation(::getInstanceFromOtherPlaces))
    
            var called = 0
            fun getInstanceFromOtherPlaces(): TestClass  {
                val v = called++
                return object : TestClass {
                    override fun getResult(): Int = v
                }
            }
        """
    ) {
        classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
            assertEquals(0, runFunction<Int>("getResult"))
            assertEquals(1, runFunction<Int>("getResult"))
        }
    }

    @Test
    fun `by function reference with default value`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }

            object TestObject : TestClass by (dynamicDelegation(::getInstanceFromOtherPlaces))
    
            var called = 0
            fun getInstanceFromOtherPlaces(default: Int = 0): TestClass  {
                val v = called++
                return object : TestClass {
                    override fun getResult(): Int = v
                }
            }
        """
    ) {
        classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
            assertEquals(0, runFunction<Int>("getResult"))
            assertEquals(1, runFunction<Int>("getResult"))
        }
    }

    @Test
    fun `by function reference inside object`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }

            object TestObject : TestClass by dynamicDelegation(TestObject::getInstanceFromOtherPlaces) {
                var called = 0
                fun getInstanceFromOtherPlaces(default: Int = 0): TestClass  {
                    val v = called++
                    return object : TestClass {
                        override fun getResult(): Int = v
                    }
                }
            }
        """
    ) {
        classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
            assertEquals(0, runFunction<Int>("getResult"))
            assertEquals(1, runFunction<Int>("getResult"))
        }
    }

    @Test
    fun `by function reference with argument inside object`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }

            var obj = TestObject
            object TestObject : TestClass by dynamicDelegation(obj::getInstanceFromOtherPlaces) {
                var called = 0
                fun getInstanceFromOtherPlaces(default: Int = 0): TestClass  {
                    val v = called++
                    return object : TestClass {
                        override fun getResult(): Int = v
                    }
                }
            }
        """
    ) {
        classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
            assertEquals(0, runFunction<Int>("getResult"))
            assertEquals(1, runFunction<Int>("getResult"))
        }
    }

    @Test
    fun `by property reference without backing field`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }

            object TestObject : TestClass by dynamicDelegation(::getInstanceFromOtherPlaces)
        
            var called = 0
            val getInstanceFromOtherPlaces : TestClass get() {
                val v = called++
                return object : TestClass {
                    override fun getResult(): Int = v
                }
            }
        """
    ) {
        classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
            assertEquals(0, runFunction<Int>("getResult"))
            assertEquals(1, runFunction<Int>("getResult"))
        }
    }

    @Test
    fun `by property reference with backing field`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }

            object TestObject : TestClass by dynamicDelegation(::getInstanceFromOtherPlaces) {
                fun updateProperty() {
                    getInstanceFromOtherPlaces = object : TestClass {
                        val v = called++
                        override fun getResult(): Int = v
                    }
                }
            }
        
            var called = 0
            var getInstanceFromOtherPlaces : TestClass = object : TestClass {
                val v = called++
                override fun getResult(): Int = v
            }
        """
    ) {
        classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
            assertEquals(0, runFunction<Int>("getResult"))
            runFunction<Unit>("updateProperty")
            assertEquals(1, runFunction<Int>("getResult"))
        }
        classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
            assertEquals(1, runFunction<Int>("getResult"))
            assertEquals(1, runFunction<Int>("getResult"))
        }
    }

    @Test
    fun `by variable reference to property reference, should evaluate variable on compilation (perquisite)`() =
        testJvmCompile(
            """
            interface TestClass {
                fun getResult(): Int
            }
            
            val variable = ::getInstanceFromOtherPlaces
            object TestObject : TestClass by dynamicDelegation(variable)
        
            var called = 0
            val getInstanceFromOtherPlaces : TestClass get() {
                val v = called++
                return object : TestClass {
                    override fun getResult(): Int = v
                }
            }
        """
        ) {
            classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
                assertEquals(0, runFunction<Int>("getResult"))
                assertEquals(1, runFunction<Int>("getResult"))
            }
        }


    @Test
    fun `by variable reference to property reference, should evaluate variable on compilation`() = testJvmCompile(
        """
            interface TestClass {
                fun getResult(): Int
            }
            
            var variable = ::getInstanceFromOtherPlaces
            object TestObject : TestClass by dynamicDelegation(variable) {
                fun updateVariable() {
                    variable = ::getInstanceFromOtherPlaces2
                }
            }
        
            var called = 0
            val getInstanceFromOtherPlaces : TestClass get() {
                val v = called++
                return object : TestClass {
                    override fun getResult(): Int = v
                }
            }
            val getInstanceFromOtherPlaces2 : TestClass get() = error("Test failed")
        """
    ) {
        classLoader.loadClass("TestObject").getDeclaredField("INSTANCE").get(null).run {
            assertEquals(0, runFunction<Int>("getResult"))
            assertEquals(1, runFunction<Int>("getResult"))
            runFunction<Unit>("updateVariable")
            assertEquals(2, runFunction<Int>("getResult"))
        }
    }

}
