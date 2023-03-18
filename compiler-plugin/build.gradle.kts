plugins {
    id("me.him188.maven-central-publish")
    kotlin("jvm")
    java
    kotlin("kapt")
    id("com.github.johnrengelman.shadow")
}

dependencies includeInShadow@{
    implementation(project(":kotlin-dynamic-delegation"))
}

dependencies compileOnly@{
    compileOnly(kotlin("stdlib")) // don't include stdlib in shadow
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")
    kapt("com.google.auto.service:auto-service:1.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")
}

dependencies tests@{
    testImplementation(kotlin("test"))
    testImplementation(project(":kotlin-dynamic-delegation"))

    testImplementation(kotlin("reflect"))

    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.kotlin}")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}") // for debugger
    //testImplementation("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")

    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0") {
        exclude("org.jetbrains.kotlin", "kotlin-annotation-processing-embeddable")
    }
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:1.8.0")

    testImplementation("org.assertj:assertj-core:3.22.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

embeddableCompiler {
    archiveFileName.set("${project.name}-all.jar")
}
repositories {
    mavenCentral()
}

val test by tasks.getting(Test::class) {
    dependsOn(tasks.getByName("embeddable"))
    this.classpath += tasks.getByName("embeddable").outputs.files
    this.classpath =
        files(*this.classpath.filterNot {
            it.absolutePath.replace("\\", "/").removeSuffix("/").endsWith(("build/classes/kotlin/main"))
        }.toTypedArray())
}

mavenCentralPublish {
    useCentralS01()
    workingDir = rootProject.buildDir.resolve("publishing")
    singleDevGithubProject("Him188", "kotlin-dynamic-delegation")
    licenseApacheV2()
}