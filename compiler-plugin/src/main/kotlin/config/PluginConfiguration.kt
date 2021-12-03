package me.him188.kotlin.dynamic.delegation.compiler.config

interface PluginConfiguration {
    companion object {
        val Default = PluginConfigurationImpl()
    }
}