import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("net.mamoe.maven-central-publish")
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(project(":kotlin-dynamic-delegation-compiler"))
}

embeddableCompiler()

mavenCentralPublish {
    workingDir = rootProject.buildDir.resolve("publishing")
    packageGroup = Versions.publicationGroup
    singleDevGithubProject("Him188", "kotlin-dynamic-delegation")
    licenseApacheV2()

    addProjectComponents = false
    publication {
        artifact(tasks.getByName("embeddable") as ShadowJar)
    }
}