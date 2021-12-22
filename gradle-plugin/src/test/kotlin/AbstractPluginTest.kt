package me.him188.kotlin.dynamic.delegation.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

abstract class AbstractPluginTest {
    companion object {
        const val kotlinVersion = "1.6.0"
    }

    fun assertGradleTaskSuccess(dir: File, taskName: String, resultAction: BuildResultScope.() -> Unit = {}) {
        assertGradleTaskOutcome(dir, taskName, TaskOutcome.SUCCESS, resultAction)
    }

    class BuildResultScope(
        private val delegate: BuildResult,
        private val taskName: String
    ) : BuildResult by delegate {
        val task = delegate.task(":$taskName")!!
    }

    fun assertGradleTaskOutcome(
        dir: File,
        taskName: String,
        outcome: TaskOutcome,
        resultAction: BuildResultScope.() -> Unit = {}
    ) {
        val result = runGradleBuild(dir, taskName, expectFailure = outcome == TaskOutcome.FAILED)
        assertEquals(outcome, result.task(":$taskName")!!.outcome)
        resultAction(BuildResultScope(result, taskName))
    }


    fun runGradleBuild(dir: File, vararg taskNames: String, expectFailure: Boolean = false): BuildResult {
        val result = GradleRunner.create()
            .withProjectDir(dir)
            .withArguments(
                "clean",
                "build",
                *taskNames,
                "--stacktrace",
            )
            .withGradleVersion("7.3.1")
            .withPluginClasspath()
            .forwardOutput()
            .withEnvironment(System.getenv())
            .runCatching {
                if (expectFailure) buildAndFail()
                else build()
            }.onFailure {
                println("Failed to ${taskNames.joinToString { "'$it'" }}")
                dir.walk().forEach { println(it) }
            }.getOrThrow()
        return result
    }


}


abstract class AbstractCompilerPluginTest : AbstractPluginTest() {
    @TempDir
    lateinit var tempDir: File

    lateinit var buildFile: File
    lateinit var settingsFile: File
    lateinit var propertiesFile: File


    @BeforeEach
    fun setup() {
        settingsFile = File(tempDir, "settings.gradle")
        settingsFile.delete()
        settingsFile.writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                }
            }
        """
        )

        propertiesFile = File(tempDir, "gradle.properties")
        propertiesFile.delete()

        buildFile = File(tempDir, "build.gradle")
        buildFile.delete()
        buildFile.writeText(
            """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '$kotlinVersion'
                id 'me.him188.kotlin-dynamic-delegation'
            }
            repositories {
                mavenCentral()
                mavenLocal()
            }
        """
        )
    }
}