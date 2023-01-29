import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    id("org.jetbrains.intellij") version "1.12.0"
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    api(project(":kotlin-dynamic-delegation"))
    api(project(":kotlin-dynamic-delegation-compiler"))

}

version = Versions.idePlugin

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set(Versions.intellij)
    downloadSources.set(true)
    updateSinceUntilBuild.set(false)

    sandboxDir.set(projectDir.resolve("run/idea-sandbox").absolutePath)

    plugins.set(
        listOf(
//            "org.jetbrains.kotlin:211-1.5.30-M1-release-141-IJ7442.40@eap",
            "java",
            "org.jetbrains.kotlin"
        )
    )
}

tasks.getByName("publishPlugin", org.jetbrains.intellij.tasks.PublishPluginTask::class) {
    val pluginKey = project.findProperty("jetbrains.hub.key")?.toString()
    if (pluginKey != null) {
        logger.info("Found jetbrains.hub.key")
        token.set(pluginKey)
    } else {
        logger.info("jetbrains.hub.key not found")
    }
}

tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask> {
    pluginDescription.set(
        """
            Kotlin compiler plugin that allows class delegation to be dynamic like property delegations.
            
            This IDE plugin provides inspections and highlighting only. You will need a compiler plugin for complete feature support.
            
            The compiler plugin supports Gradle. Add the following code into build.gradle.kts:
            
            <pre>
            plugins {
                id("me.him188.kotlin-dynamic-delegation") version "VERSION"
            }
            </pre>
            
            where VERSION can be found <a href="https://github.com/Him188/kotlin-dynamic-delegation/releases">here</a>, e.g. "0.1.1-160.1".
        """.trimIndent()
    )
    changeNotes.set(
        """
        See <a href="https://github.com/Him188/kotlin-dynamic-delegation/releases">Release notes</a>
    """.trimIndent()
    )
}

tasks.withType(KotlinJvmCompile::class) {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
}