package example.kotlin.compiler.plugin.template

interface IPluginConfiguration {
    companion object {
        val Default = PluginConfigurationImpl()
    }
}