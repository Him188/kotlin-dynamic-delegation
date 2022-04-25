# kotlin-dynamic-delegation

Kotlin compiler plugin that allows class delegation to be dynamic like property delegations.

## Features

### Dynamic Delegations

The plugin provides:

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

The compiler generates `TestObject.getResult()` as follows:

```kotlin
override fun getResult(): Int = getInstanceFromOtherPlaces().getResult()
```

So the delegated instance can be changed.

### Using Lazy In Functions

Functions can also be 'lazy' by making a value lazy and persistent.

```kotlin
override fun toString(): String = persistent { this.joinToString() } // initialize lazily once and use afterwards.
```

is the same as

```kotlin
private val toStringTemp by lazy { this.joinToString() }
override fun toString(): String = toStringTemp
```

## Using the plugin

build.gradle.kts

```kotlin
plugins {
    id("me.him188.kotlin-dynamic-delegation") version "VERSION"
}
```

The plugin adds a 'compileOnly' dependency 'me.him188:kotlin-dynamic-delegation-runtime'.

See VERSION from [releases](https://github.com/Him188/kotlin-dynamic-delegation/releases)

## Installing IntelliJ IDEA plugin

IDEA plugin can help to edit code and to report errors before compiling the code. By seeing the type cast hint(see
picture below), you will know, without compiling, that your code with dynamic delegations will work.

Plugin Marketplace Page: https://plugins.jetbrains.com/plugin/18219-kotlin-dynamic-delegation

![](.README_images/57c295e7.png)