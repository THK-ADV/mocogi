package providers

import parsing.modulekeys.ModuleKeyParser
import service.core.ModuleKeyService

import javax.inject.{Inject, Provider, Singleton}

@Singleton
final class ModuleKeyServiceProvider @Inject() (
    moduleKeyParser: ModuleKeyParser
) extends Provider[ModuleKeyService] {
  override def get() =
    ModuleKeyService(moduleKeyParser, "conf/module_keys.yaml")
}
