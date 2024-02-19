package providers

import catalog.ModuleCatalogConfig
import git.Branch

import javax.inject.{Inject, Provider, Singleton}

@Singleton
final class ModuleCatalogConfigProvider @Inject() (configReader: ConfigReader)
    extends Provider[ModuleCatalogConfig] {
  override def get() = ModuleCatalogConfig(
    configReader.tmpFolderPath,
    configReader.moduleCatalogOutputFolderPath,
    configReader.moduleCatalogLabel,
    configReader.gitModuleCatalogsFolder,
    Branch(configReader.mainBranch)
  )
}
