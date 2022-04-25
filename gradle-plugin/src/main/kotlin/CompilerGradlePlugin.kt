package me.him188.kotlin.dynamic.delegation.gradle

import me.him188.kotlin.dynamic.delegation.build.BuildConfig
import me.him188.kotlin.dynamic.delegation.compiler.DynamicDelegationCommandLineProcessor
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*


internal fun MyExtension.toSubpluginOptionList(): List<SubpluginOption> {
    return listOf(
//        SubpluginOption(PluginCompilerConfig3urationKeys.EXAMPLE.name, ),
    )
}

private val pluginArtifact = SubpluginArtifact(
    groupId = BuildConfig.GROUP_ID,
    artifactId = BuildConfig.ARTIFACT_ID_COMPILER_EMBEDDABLE,
    version = BuildConfig.VERSION
)

@Suppress("unused") // used in build.gradle.kts
public open class CompilerGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {

        kotlin.runCatching { target.extensions.getByType(KotlinMultiplatformExtension::class.java) }
            .fold(onSuccess = { kotlin ->
                // when MPP

                val applicableTargets =
                    kotlin.targets.filter { it.platformType == KotlinPlatformType.common }

                for (applicableTarget in applicableTargets) {
                    applicableTarget.compilations.flatMap { it.allKotlinSourceSets }.forEach {
                        it.dependencies {
                            implementation("${BuildConfig.GROUP_ID}:${BuildConfig.ARTIFACT_ID_RUNTIME}:${BuildConfig.VERSION}")
                        }
                    }
                }
                if (applicableTargets.isNotEmpty()) {
                    target.repositories.mavenCentral()
                }

            }, onFailure = {
                if (kotlin.runCatching { target.extensions.getByType(KotlinJvmProjectExtension::class.java) }.isSuccess) {
                    // when JVM
                    target.dependencies.add(
                        "compileOnly",
                        "${BuildConfig.GROUP_ID}:${BuildConfig.ARTIFACT_ID_RUNTIME}:${BuildConfig.VERSION}"
                    )
                    target.repositories.mavenCentral()
                }

                if (kotlin.runCatching { target.extensions.getByType(KotlinJsProjectExtension::class.java) }.isSuccess) {
                    // when JS
                    target.dependencies.add(
                        "compileOnly",
                        "${BuildConfig.GROUP_ID}:${BuildConfig.ARTIFACT_ID_RUNTIME}:${BuildConfig.VERSION}"
                    )
                    target.repositories.mavenCentral()
                }
            })

//        target.extensions.create("myExtension", MyExtension::class.java)
    }

    override fun getCompilerPluginId(): String = DynamicDelegationCommandLineProcessor.COMPILER_PLUGIN_ID

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val ext: MyExtension? =
            project.extensions.findByType(MyExtension::class.java)
        return project.provider {
            mutableListOf<SubpluginOption>()
            ext?.toSubpluginOptionList() ?: emptyList()
        }
    }

    override fun getPluginArtifact(): SubpluginArtifact = pluginArtifact

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return true

//        return kotlinCompilation.target.project.plugins.hasPlugin(CompilerGradlePlugin::class.java)
//                && when (kotlinCompilation.platformType) {
//            KotlinPlatformType.jvm,
//            KotlinPlatformType.androidJvm,
//            KotlinPlatformType.common,
//            -> true
//            else -> false
//        }
    }
}