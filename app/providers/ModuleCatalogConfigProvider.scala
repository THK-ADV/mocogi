package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import catalog.ModuleCatalogConfig
import git.Branch

@Singleton
final class ModuleCatalogConfigProvider @Inject() (configReader: ConfigReader) extends Provider[ModuleCatalogConfig] {
  override def get() = ModuleCatalogConfig(
    configReader.tmpFolderPath,
    configReader.moduleCatalogOutputFolderPath,
    configReader.moduleCatalogLabel,
    configReader.gitModuleCatalogsFolder,
    Branch(configReader.mainBranch)
  )
}
