# kotlin-dynamic-delegation

Kotlin compiler plugin that allows class delegation to be dynamic like property delegations.

The plugin is working in progress.

## What does this plugin do

It provides a function

```kotlin
public fun <R> dynamicDelegation(value: () -> R): R =
    throw NotImplementedError("Implemented as intrinsic")
```

This function is implemented by the compiler. You can call this function on class delegation as follows:

```kotlin
interface TestClass {
    fun getResult(): Int
}
object TestObject : TestClass by (dynamicDelegation { getInstanceFromOtherPlaces() })

var called = 0
fun getInstanceFromOtherPlaces(): TestClass {
    val v = called++
    return object : TestClass {
        override fun getResult(): Int = v // static value here!
    }
}

class Test {
    @Test
    fun test() {
        assertEquals(0, TestObject.getResult())
        assertEquals(1, TestObject.getResult())
        assertEquals(2, TestObject.getResult())
    }
}
```

Everytime `fun getResult(): Int` in `TestObject` is called, it calls the lambda `{ getInstanceFromOtherPlaces() }` to
get a `TestClass` instance.

**This eliminates your work against delegating the class manually.** 