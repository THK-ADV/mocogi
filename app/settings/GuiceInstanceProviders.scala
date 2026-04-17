package settings

import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import auth.KeycloakConfig
import cli.GitCLI
import git.Branch
import git.GitConfig
import models.ModuleKeysToReview
import service.mail.MailConfig

@Singleton
final class GitCliGuiceProvider @Inject() (settings: AppSettings) extends Provider[GitCLI] {
  override def get(): GitCLI =
    GitCLI(Branch(settings.git.draftBranch), Paths.get(settings.git.localGitFolderPath))
}

@Singleton
final class MailConfigGuiceProvider @Inject() (settings: AppSettings) extends Provider[MailConfig] {
  override def get(): MailConfig =
    MailConfig(settings.mail.sender, 5)
}

@Singleton
final class KeycloakConfigGuiceProvider @Inject() (settings: AppSettings) extends Provider[KeycloakConfig] {
  override def get(): KeycloakConfig =
    KeycloakConfig(settings.keycloak.jwksUrl, settings.keycloak.issuer)
}

@Singleton
final class GitConfigProvider @Inject() (settings: AppSettings) extends Provider[GitConfig] {
  override def get(): GitConfig =
    GitConfig(
      SecretString.unwrap(settings.git.accessToken),
      settings.git.baseUrl,
      settings.git.projectId,
      Branch(settings.git.mainBranch),
      Branch(settings.git.draftBranch),
      settings.git.modulesFolder,
      settings.git.coreFolder,
      settings.git.moduleCatalogsFolder,
      settings.git.moduleCompanionFolder,
      settings.git.autoApprovedLabel,
      settings.git.reviewRequiredLabel,
      settings.git.fastForwardLabel,
      settings.git.defaultEmail,
      settings.git.defaultUser
    )
}

@Singleton
final class ModuleKeysToReviewProvider @Inject() (settings: AppSettings) extends Provider[ModuleKeysToReview] {
  override def get(): ModuleKeysToReview =
    ModuleKeysToReview(settings.moduleKeysToReview.pavModuleKeys.toSet)
}
