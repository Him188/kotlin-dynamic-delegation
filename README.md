# kotlin-dynamic-delegation

Kotlin compiler plugin that allows class delegation to be dynamic like property delegations.

## What can this plugin do

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

The compiler generates `TestObject.getResult()` as follows:

```kotlin
override fun getResult(): Int = getInstanceFromOtherPlaces().getResult()
```

So the delegated instance can be changed.

## Using the plugin

build.gradle.kts

```kotlin
plugins {
    id("me.him188.kotlin-dynamic-delegation") version "VERSION"
}
```

See VERSION from [releases](https://github.com/Him188/kotlin-dynamic-delegation/releases)

## Installing IntelliJ plugin

Plugin for IntelliJ IDEA is provided to help writing dynamic delegations. It provides various inspections that report
before compiling the code.

Plugin Marketplace Page: https://plugins.jetbrains.com/plugin/18219-kotlin-dynamic-delegation

![](.README_images/57c295e7.png)