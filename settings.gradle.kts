pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}

rootProject.name = "kotlin-dynamic-delegation"

includeProject("kotlin-dynamic-delegation", "runtime")
includeProject("kotlin-dynamic-delegation-compiler", "compiler-plugin")
includeProject("kotlin-dynamic-delegation-compiler-embeddable", "compiler-plugin-embeddable")
includeProject("kotlin-dynamic-delegation-gradle", "gradle-plugin")
includeProject("kotlin-dynamic-delegation-intellij", "ide-plugin")

fun includeProject(name: String, path: String) {
    include(path)
    project(":$path").name = name
}