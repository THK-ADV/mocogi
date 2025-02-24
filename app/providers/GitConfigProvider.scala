package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import git.Branch
import git.GitConfig

@Singleton
final class GitConfigProvider @Inject() (config: ConfigReader) extends Provider[GitConfig] {
  override def get(): GitConfig =
    GitConfig(
      config.gitToken,
      config.accessToken,
      config.baseUrl,
      config.projectId,
      Branch(config.mainBranch),
      Branch(config.draftBranch),
      config.gitModulesFolder,
      config.gitCoreFolder,
      config.gitModuleCatalogsFolder,
      config.gitModuleCompanionFolder,
      config.autoApprovedLabel,
      config.reviewRequiredLabel,
      config.defaultEmail,
      config.defaultUser
    )
}
