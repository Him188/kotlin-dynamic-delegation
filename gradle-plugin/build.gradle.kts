plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.github.gmazzo.buildconfig")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin-api"))
    compileOnly(kotlin("gradle-plugin"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.kotlin}")

    api(project(":kotlin-dynamic-delegation-compiler"))
}

gradlePlugin {
    plugins {
        create("kotlinDynamicDelegation") {
            id = "me.him188.kotlin.dynamic.delegation"
            displayName = "Kotlin Dynamic Delegation"
            description = project.description
            implementationClass = "me.him188.kotlin.dynamic.delegation.CompilerGradlePlugin"
        }
    }
}

buildConfig {
    val project = project(":kotlin-dynamic-delegation-compiler")
    packageName("me.him188.kotlin.dynamic.delegation.build")
    buildConfigField("String", "PLUGIN_ID", "\"me.him188.kotlin-dynamic-delegation\"")
    buildConfigField("String", "GROUP_ID", "\"${project.group}\"")
    buildConfigField("String", "ARTIFACT_ID", "\"${project.name}\"")
    buildConfigField(
        "String",
        "ARTIFACT_ID_EMBEDDABLE",
        "\"${project(":kotlin-dynamic-delegation-compiler-embeddable").name}\""
    )
    buildConfigField("String", "VERSION", "\"${project.version}\"")
}

/*
tasks.getByName("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier.set("")
}

tasks.publishPlugins.get().dependsOn(tasks.shadowJar.get())*/