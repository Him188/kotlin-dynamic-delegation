import me.him188.kotlin.dynamic.delegation.dynamicDelegation

interface I
class C : I by (dynamicDelegation { TODO() })

fun main() {
    //
    //

    //
    dynamicDelegation {
        object : I {}
    }
}