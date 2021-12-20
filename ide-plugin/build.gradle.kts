import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    id("org.jetbrains.intellij") version "1.3.0"
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation(project(":kotlin-dynamic-delegation"))
    implementation(project(":kotlin-dynamic-delegation-compiler"))
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
            
            Provides inspections highlighting.
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