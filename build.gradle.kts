@file:Suppress("UnstableApiUsage", "LocalVariableName")

import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

plugins {
    kotlin("multiplatform") apply false
    id("net.mamoe.maven-central-publish") version "0.8.0" apply false
    kotlin("kapt") apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("com.gradle.plugin-publish") version "0.12.0" apply false
    id("io.codearte.nexus-staging") version "0.22.0"
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.0" apply false
    id("com.github.gmazzo.buildconfig") version "3.0.3" apply false
}

allprojects {
    group = Versions.publicationGroup
    description =
        "Kotlin compiler plugin for generating blocking bridges for calling suspend functions from Java with minimal effort"
    version = Versions.project

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

nexusStaging {
    packageGroup = rootProject.group.toString()
    username = System.getProperty("sonatype_key") ?: project.findProperty("sonatype.key")?.toString()
    password = System.getProperty("sonatype_password") ?: project.findProperty("sonatype.password")?.toString()
}

subprojects {
    afterEvaluate {
        setupKotlinSourceSetsSettings()
    }
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

fun Project.setupKotlinSourceSetsSettings() {
    kotlin.runCatching {
        extensions.getByType(KotlinProjectExtension::class.java)
    }.getOrNull()?.run {
        sourceSets.all {

            languageSettings.apply {
                progressiveMode = true

                optIn("kotlin.Experimental")
                optIn("kotlin.RequiresOptIn")

                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlin.experimental.ExperimentalTypeInference")
                optIn("kotlin.contracts.ExperimentalContracts")
            }
        }
    }

    kotlin.runCatching {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    kotlin.runCatching { tasks.getByName("test", Test::class) }.getOrNull()?.apply {
        useJUnitPlatform()
    }
}