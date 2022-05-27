plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.github.gmazzo.buildconfig") version "3.0.3"
    `maven-publish`
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(gradleApi())
    implementation(kotlin("gradle-plugin"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.kotlin}")

    api(project(":kotlin-dynamic-delegation-compiler"))


    testImplementation(gradleApi())
    testImplementation(localGroovy())
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation(gradleTestKit())

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

kotlin {
    explicitApi()
}

gradlePlugin {
    plugins {
        create("kotlinDynamicDelegation") {
            id = "me.him188.kotlin-dynamic-delegation"
            displayName = "Kotlin Dynamic Delegation"
            description = project.description
            implementationClass = "me.him188.kotlin.dynamic.delegation.gradle.CompilerGradlePlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/him188/kotlin-dynamic-delegation"
    vcsUrl = "https://github.com/him188/kotlin-dynamic-delegation.git"
    tags = listOf("build")
}

tasks.withType<Test> {
    dependsOn("publishToMavenLocal", ":kotlin-dynamic-delegation:publishToMavenLocal")
}

buildConfig {
    val project = project(":kotlin-dynamic-delegation-compiler")
    packageName("me.him188.kotlin.dynamic.delegation.build")
    useKotlinOutput {
        internalVisibility = true
    }
    buildConfigField("String", "PLUGIN_ID", "\"me.him188.kotlin-dynamic-delegation\"")
    buildConfigField("String", "GROUP_ID", "\"${project.group}\"")
    buildConfigField("String", "ARTIFACT_ID_COMPILER", "\"${project(":kotlin-dynamic-delegation-compiler").name}\"")
    buildConfigField(
        "String",
        "ARTIFACT_ID_COMPILER_EMBEDDABLE",
        "\"${project(":kotlin-dynamic-delegation-compiler-embeddable").name}\""
    )
    buildConfigField(
        "String",
        "ARTIFACT_ID_RUNTIME",
        "\"${project(":kotlin-dynamic-delegation").name}\""
    )
    buildConfigField("String", "VERSION", "\"${project.version}\"")
}

/*
tasks.getByName("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier.set("")
}

tasks.publishPlugins.get().dependsOn(tasks.shadowJar.get())*/