import me.him188.kotlin.jvm.blocking.bridge.compiler.UnitCoercion.COMPATIBILITY
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    id("me.him188.kotlin-dynamic-delegation") version "1.11.0"
}

blockingBridge {
//    enableForModule = true

    unitCoercion = COMPATIBILITY
}

group = "me.him188"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }

}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
}