package providers

import git.{Branch, GitConfig}

import javax.inject.{Inject, Provider, Singleton}

@Singleton
final class GitConfigProvider @Inject() (config: ConfigReader)
    extends Provider[GitConfig] {
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
      config.autoApprovedLabel,
      config.reviewRequiredLabel,
      config.defaultEmail,
      config.defaultUser
    )
}
