package compiler

import org.jetbrains.kotlin.config.CompilerConfiguration

internal abstract class AbstractCompilerTest {
    protected open val overrideCompilerConfiguration: CompilerConfiguration? = null

}