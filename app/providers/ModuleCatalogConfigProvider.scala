package providers

import catalog.ModuleCatalogConfig
import models.Branch

import javax.inject.{Inject, Provider, Singleton}

@Singleton
final class ModuleCatalogConfigProvider @Inject() (configReader: ConfigReader)
    extends Provider[ModuleCatalogConfig] {
  override def get() = ModuleCatalogConfig(
    configReader.tmpFolderPath,
    configReader.moduleCatalogFolderPath,
    configReader.repoPath,
    configReader.mcPath,
    configReader.pushScriptPath,
    Branch(configReader.mainBranch),
    configReader.moduleCatalogLabel
  )
}
