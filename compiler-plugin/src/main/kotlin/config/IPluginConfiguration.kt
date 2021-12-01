package example.kotlin.compiler.plugin.template.compiler.config

interface IPluginConfiguration {
    companion object {
        val Default = PluginConfigurationImpl()
    }
}