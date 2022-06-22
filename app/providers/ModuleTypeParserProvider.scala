package providers

import parsing.metadata.{ModuleTypeParser, ModuleTypeParserImpl}

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class ModuleTypeParserProvider @Inject() (config: ConfigReader)
    extends Provider[ModuleTypeParser] {
  override def get() = new ModuleTypeParserImpl(config.moduleTypes)
}
