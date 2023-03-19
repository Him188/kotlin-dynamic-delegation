import me.him188.kotlin.dynamic.delegation.dynamicDelegation

interface I
class C : I by (dynamicDelegation { TODO() })

interface TestClass {
    val result: Int
}

class IncompatibleClass {
    val refined: TestClass get() = error("")
}

interface CommandManager {
    val commands: List<Command>
    fun register(command: Command)

    companion object : CommandManager by (dynamicDelegation { instance }) {
    }
}

private val instance by lazy { CommandManagerImpl() }

interface Command
class CommandManagerImpl : CommandManager {
    override val commands: List<Command>
        get() = listOf()

    override fun register(command: Command) {
        TODO("Not yet implemented")
    }
}

fun main() {
    println(CommandManager.commands)
}


//class CombinedMessage : TestClass by dynamicDelegation(IncompatibleClass::refined)