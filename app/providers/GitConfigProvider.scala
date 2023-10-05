package providers

import git.GitConfig

import javax.inject.{Inject, Provider, Singleton}

@Singleton()
final class GitConfigProvider @Inject() (config: ConfigReader)
    extends Provider[GitConfig] {
  override def get(): GitConfig =
    GitConfig(
      config.gitToken,
      config.accessToken,
      config.moduleModeToken,
      config.baseUrl,
      config.projectId,
      config.mainBranch,
      config.draftBranch,
      config.modulesRootFolder,
      config.coreRootFolder,
      config.autoApprovedLabel,
      config.reviewApprovedLabel
    )
}
