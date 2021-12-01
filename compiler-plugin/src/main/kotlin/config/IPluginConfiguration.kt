package example.kotlin.compiler.plugin.template.config

interface IPluginConfiguration {
    companion object {
        val Default = PluginConfigurationImpl()
    }
}