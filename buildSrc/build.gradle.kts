plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.Experimental")
        languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
    }
}

dependencies {
    compileOnly(gradleApi())
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:${version("kotlin")}")

    compileOnly("com.github.jengelman.gradle.plugins:shadow:6.0.0")
}

fun version(name: String): String = project.rootDir.resolve("src/main/kotlin/Versions.kt").readText()
    .substringAfter("$name = \"", "")
    .substringBefore("\"", "")
    .also {
        check(it.isNotBlank()) { "Cannot find version $name" }
    }