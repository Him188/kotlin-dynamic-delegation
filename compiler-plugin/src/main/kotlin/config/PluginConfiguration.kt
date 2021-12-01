package example.kotlin.compiler.plugin.template.compiler.config

interface PluginConfiguration {
    companion object {
        val Default = PluginConfigurationImpl()
    }
}