package providers

import git.GitConfig
import models.Branch

import javax.inject.{Inject, Provider, Singleton}

@Singleton
final class GitConfigProvider @Inject() (config: ConfigReader)
    extends Provider[GitConfig] {
  override def get(): GitConfig =
    GitConfig(
      config.gitToken,
      config.accessToken,
      config.moduleModeToken,
      config.baseUrl,
      config.projectId,
      Branch(config.mainBranch),
      Branch(config.draftBranch),
      config.modulesRootFolder,
      config.coreRootFolder,
      config.moduleCatalogRootFolder,
      config.autoApprovedLabel,
      config.reviewRequiredLabel,
      config.defaultEmail,
      config.defaultUser
    )
}
