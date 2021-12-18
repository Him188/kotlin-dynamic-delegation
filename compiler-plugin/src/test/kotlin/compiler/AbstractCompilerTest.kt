package compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import me.him188.kotlin.dynamic.delegation.compiler.PluginComponentRegistrar
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import java.io.File
import java.util.*
import kotlin.test.assertEquals

internal abstract class AbstractCompilerTest {
    protected open val overrideCompilerConfiguration: CompilerConfiguration? = null


    sealed class KotlinCompilationTestBuilder {
        private val kotlinSources = mutableListOf<String>()
        private val javaSources = mutableListOf<String>()

        private var expectSuccess = true
        private var resultHandler: (KotlinCompilation.Result.() -> Unit)? = null

        private val compilationActions = mutableListOf<KotlinCompilation.() -> Unit>()

        fun javaSource(@Language("java") code: String) {
            this.javaSources.add(code)
        }

        fun kotlinSource(@Language("kt") code: String) {
            this.kotlinSources.add(code)
        }

        fun KotlinCompilation.useOldBackend() {
            useIR = false
            useOldBackend = true
        }

        fun useOldBackend() = compilation { useOldBackend() }

        fun compilation(block: KotlinCompilation.() -> Unit) {
            compilationActions.add(block)
        }

        fun expectFailure(block: KotlinCompilation.Result.() -> Unit): FinishConfiguration {
            expectSuccess = false
            resultHandler = block
            return FinishConfiguration
        }

        fun expectSuccess(block: KotlinCompilation.Result.() -> Unit): FinishConfiguration {
            expectSuccess = true
            resultHandler = block
            return FinishConfiguration
        }

        fun run() {
            val builder = this
            testJvmCompile(
                builder.kotlinSources.joinToString(separator = FILE_SPLITTER),
                builder.javaSources.joinToString(separator = FILE_SPLITTER).takeIf { it.isNotBlank() },
                expectSuccess = builder.expectSuccess,
                config = {
                    builder.compilationActions.forEach { it.invoke(this) }
                }
            ) {
                println("Compiler output: \n$messages")
                builder.resultHandler?.invoke(this)
            }
        }
    }

    object FinishConfiguration

    class KotlinJvmCompilationTestBuilder : KotlinCompilationTestBuilder()

    data class MessageLine(
        val kind: Kind?,
        val message: String,
        val file: String,
        val location: String,
    ) {
        enum class Kind(val short: String) {
            WARNING("w"),
            ERROR("e"),
            ;

            companion object {
                fun fromStringOrNull(string: String): Kind? {
                    return enumValues<Kind>().find {
                        string.startsWith(it.short)
                    }
                }
            }
        }
    }

    class KotlinCompilationMessages(val delegate: List<MessageLine>)

    fun KotlinCompilationMessages.assertAllKind(kind: MessageLine.Kind) {
        for (line in delegate) {
            if (line.kind != kind) {
                throw AssertionError("Expect all $kind, but got $line¬")
            }
        }
    }

    fun KotlinCompilationMessages.assertSingleError(): MessageLine {
        delegate.single().run {
            assertEquals(MessageLine.Kind.ERROR, kind)
            return this
        }
    }

    fun analyzeKotlinCompilationMessages(messages: String): KotlinCompilationMessages {
        val matching = Regex("""([ew]): ([a-zA-Z0-9/.:\\\-_+\w ]+)?: \(([0-9]+?, [0-9]+?)\): (.+)""".trimMargin())
        return matching.findAll(messages)
            .map { it.destructured }
            .map { (kind, file, location, message) ->
                MessageLine(MessageLine.Kind.fromStringOrNull(kind), message, file, location)
            }
            .toList()
            .let {
                KotlinCompilationMessages(it)
            }
    }

    fun KotlinCompilation.Result.analyzeMessages(): KotlinCompilationMessages =
        analyzeKotlinCompilationMessages(messages)

    // new
    fun testJvmCompilation(action: KotlinJvmCompilationTestBuilder.() -> FinishConfiguration) {
        val builder = KotlinJvmCompilationTestBuilder().apply { action() }
        builder.run()
    }

    fun KotlinCompilation.Result.withMessages(block: KotlinCompilationMessages.() -> Unit) {
        return analyzeMessages().run(block)
    }


    companion object {
        @Suppress("ObjectPropertyName")
        const val FILE_SPLITTER = "-------------------------------------"

        // legacy
        fun testJvmCompile(
            @Language("kt")
            kt: String,
            @Language("java")
            java: String? = null,
            jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
            overrideCompilerConfiguration: CompilerConfiguration? = null,
            config: KotlinCompilation.() -> Unit = {},
            expectSuccess: Boolean = true,
            block: KotlinCompilation.Result.() -> Unit = {},
        ) {
            val intrinsicImports = listOf(
                "import kotlin.test.*",
                "import me.him188.kotlin.dynamic.delegation.*"
            )
            val kotlinSources = kt.split(Companion.FILE_SPLITTER).mapIndexed { index, source ->
                when {
                    source.trim().startsWith("package") -> {
                        SourceFile.kotlin("TestData${index}.kt", run {
                            source.trimIndent().lines().mapTo(LinkedList()) { it }
                                .apply { addAll(1, intrinsicImports) }
                                .joinToString("\n")
                        })
                    }
                    source.trim().startsWith("@file:") -> {
                        SourceFile.kotlin("TestData${index}.kt", run {
                            source.trim().trimIndent().lines().mapTo(LinkedList()) { it }
                                .apply { addAll(1, intrinsicImports) }
                                .joinToString("\n")
                        })
                    }
                    else -> {
                        SourceFile.kotlin(
                            name = "TestData${index}.kt",
                            contents = "${intrinsicImports.joinToString("\n")}\n${source.trimIndent()}"
                        )
                    }
                }
            }
            val result =
                KotlinCompilation().apply {
                    sources = listOfNotNull(
                        *kotlinSources.toTypedArray(),
                        java?.let { javaSource ->
                            SourceFile.java(
                                Regex("""class\s*(.*?)\s*\{""").find(javaSource)!!.groupValues[1].let { "$it.java" },
                                javaSource
                            )
                        }
                    )

                    compilerPlugins =
                        listOf(PluginComponentRegistrar(overrideConfigurations = overrideCompilerConfiguration))
                    verbose = false

                    this.jvmTarget = jvmTarget.description

                    workingDir = File("testCompileOutput").apply {
                        walk().forEach { it.delete() }
                        mkdir()
                    }

                    useIR = true
                    useOldBackend = false

                    inheritClassPath = true
                    messageOutputStream = System.out

                    config()
                }.compile().also { result ->
                    if (expectSuccess) {
                        assert(result.exitCode == KotlinCompilation.ExitCode.OK) {
                            "Test data compilation failed."
                        }
                    }
                }

            block(result)
        }

    }

}