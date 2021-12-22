package compiler

import com.tschuchort.compiletesting.KotlinCompilation
import me.him188.kotlin.dynamic.delegation.compiler.PluginComponentRegistrar
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.plugins.ServiceLoaderLite
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.destinationAsFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URI
import java.nio.file.Paths
import java.util.*
import kotlin.concurrent.thread
import kotlin.io.path.createTempDirectory
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.assertEquals

data class CompilationResult(
    val exitCode: ExitCode,
    val messages: String,
)


internal abstract class AbstractCompilerTest {
    protected open val overrideCompilerConfiguration: CompilerConfiguration? = null


    sealed class KotlinCompilationTestBuilder {
        private val kotlinSources = mutableListOf<String>()
        private val javaSources = mutableListOf<String>()

        private var expectSuccess = true
        private var resultHandler: (CompilationResult.() -> Unit)? = null

        private val compilationActions = mutableListOf<K2JVMCompilerArguments.() -> Unit>()

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

        fun useOldBackend() = compilation { useOldBackend = true }

        fun compilation(block: K2JVMCompilerArguments.() -> Unit) {
            compilationActions.add(block)
        }

        fun expectFailure(block: CompilationResult.() -> Unit): FinishConfiguration {
            expectSuccess = false
            resultHandler = block
            return FinishConfiguration
        }

        fun expectSuccess(block: CompilationResult.() -> Unit): FinishConfiguration {
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

    fun CompilationResult.analyzeMessages(): KotlinCompilationMessages =
        analyzeKotlinCompilationMessages(messages)

    // new
    fun testJvmCompilation(action: KotlinJvmCompilationTestBuilder.() -> FinishConfiguration) {
        val builder = KotlinJvmCompilationTestBuilder().apply { action() }
        builder.run()
    }

    fun CompilationResult.withMessages(block: KotlinCompilationMessages.() -> Unit) {
        return analyzeMessages().run(block)
    }


    companion object {
        @Suppress("ObjectPropertyName")
        const val FILE_SPLITTER = "-------------------------------------"
        private val tempDir = createTempDirectory("kotlin-compile-test").also {
            Runtime.getRuntime().addShutdownHook(thread(false) {
                kotlin.runCatching { it.toFile().deleteRecursively() }
            })
        }

        data class SourceFile(
            val name: String,
            val content: String
        )

        // legacy
        fun testJvmCompile(
            @Language("kt")
            kt: String,
            @Language("java")
            java: String? = null,
            jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
            overrideCompilerConfiguration: CompilerConfiguration? = null,
            config: K2JVMCompilerArguments.() -> Unit = {},
            expectSuccess: Boolean = true,
            block: CompilationResult.() -> Unit = {},
        ) {
            val intrinsicImports = listOf(
                "import kotlin.test.*",
                "import me.him188.kotlin.dynamic.delegation.*"
            )
            val kotlinSources = kt.split(Companion.FILE_SPLITTER).mapIndexed { index, source ->
                when {
                    source.trim().startsWith("package") -> {
                        SourceFile("TestData${index}.kt", run {
                            source.trimIndent().lines().mapTo(LinkedList()) { it }
                                .apply { addAll(1, intrinsicImports) }
                                .joinToString("\n")
                        })
                    }
                    source.trim().startsWith("@file:") -> {
                        SourceFile("TestData${index}.kt", run {
                            source.trim().trimIndent().lines().mapTo(LinkedList()) { it }
                                .apply { addAll(1, intrinsicImports) }
                                .joinToString("\n")
                        })
                    }
                    else -> {
                        SourceFile(
                            "TestData${index}.kt",
                            "${intrinsicImports.joinToString("\n")}\n${source.trimIndent()}"
                        )
                    }
                }
            }

            val messageBuffer = ByteArrayOutputStream()

            val dir = tempDir.resolve("test" + Random.nextInt().absoluteValue.toString()).toFile().apply { mkdirs() }

            fun getResourcesPath(): String {
                val resourceName = "META-INF/services/org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar"
                return this::class.java.classLoader.getResources(resourceName)
                    .asSequence()
                    .mapNotNull { url ->
                        val uri = URI.create(url.toString().removeSuffix("/$resourceName"))
                        when (uri.scheme) {
                            "jar" -> Paths.get(URI.create(uri.schemeSpecificPart.removeSuffix("!")))
                            "file" -> Paths.get(uri)
                            else -> return@mapNotNull null
                        }.toAbsolutePath()
                    }
                    .find { resourcesPath ->
                        ServiceLoaderLite.findImplementations(
                            ComponentRegistrar::class.java,
                            listOf(resourcesPath.toFile())
                        )
                            .any { implementation -> implementation == PluginComponentRegistrar::class.java.name }
                    }?.toString()
                    ?: throw AssertionError("Could not get path to ComponentRegistrar service from META-INF")
            }


            val exitCode = K2JVMCompiler().run {
                val arguments = createArguments().apply(config)
                val collector = PrintingMessageCollector(
                    PrintStream(CombinedOutputStream(System.out, messageBuffer)),
                    MessageRenderer.GRADLE_STYLE,
                    true
                )
                arguments.allowNoSourceFiles = true
                arguments.pluginClasspaths = arrayOf(getResourcesPath())
                arguments.destinationAsFile = File("testCompileOutput").apply {
                    walk().forEach { it.delete() }
                    mkdir()
                }
                arguments.jvmTarget = jvmTarget.description

                kotlinSources.associateBy { dir.resolve(it.name) }.forEach { (file, source) ->
                    file.writeText(source.content)
                    arguments.freeArgs += file.absolutePath
                }
                exec(collector, Services.EMPTY, arguments)
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