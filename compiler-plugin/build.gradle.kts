plugins {
    id("me.him188.maven-central-publish")
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow")
}

dependencies includeInShadow@{
    implementation(project(":kotlin-dynamic-delegation"))
}

dependencies compileOnly@{
    implementation("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")
    kapt("com.google.auto.service:auto-service:1.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")
}

dependencies tests@{
    testImplementation(kotlin("reflect"))
    testImplementation(kotlin("test-junit5"))

    testImplementation(project(":kotlin-dynamic-delegation"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}") // for debugger
    //testImplementation("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")


    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.6")
    testImplementation("org.assertj:assertj-core:3.21.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

embeddableCompiler()

val test by tasks.getting(Test::class) {
    dependsOn(tasks.getByName("embeddable"))
    this.classpath += tasks.getByName("embeddable").outputs.files
    this.classpath =
        files(*this.classpath.filterNot {
            it.absolutePath.replace("\\", "/").removeSuffix("/").endsWith(("build/classes/kotlin/main"))
        }.toTypedArray())
}

mavenCentralPublish {
    workingDir = rootProject.buildDir.resolve("publishing")
    singleDevGithubProject("Him188", "kotlin-dynamic-delegation")
    licenseApacheV2()
}