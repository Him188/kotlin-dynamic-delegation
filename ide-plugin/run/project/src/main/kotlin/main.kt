import me.him188.kotlin.dynamic.delegation.dynamicDelegation

interface I
class C : I by (dynamicDelegation { TODO() })

fun main() {
    //
    //

    //
//    dynamicDelegation {
//        object : I {}
//    }
}

interface TestClass {
    val result: Int
}

class IncompatibleClass {
    val refined: TestClass get() = error("")
}

//class CombinedMessage : TestClass by dynamicDelegation(IncompatibleClass::refined)