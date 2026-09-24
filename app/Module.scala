import java.nio.file.Paths

import scala.annotation.unused

import auth.KeycloakConfig
import cli.GitCLI
import cli.MarkdownCLI
import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import git.publisher.ModulePublisher
import git.subscriber.ModuleDatabaseActor
import git.subscriber.ModuleSubscribers
import git.Branch
import git.GitConfig
import models.ModuleKeysToReview
import parsing.metadata.MetadataParser
import parsing.metadata.THKV1Parser
import play.api.libs.concurrent.PekkoGuiceSupport
import play.api.Configuration
import play.api.Environment
import printing.yaml.MetadataYamlPrinter
import providers.ModuleSubscribersProvider
import service.image.PeopleImageUpdateActor
import service.mail.MailActor
import service.mail.MailConfig
import service.notification.ReviewNotificationActor
import settings.AppSettings
import settings.ExamListPathsSettings
import settings.ModuleCatalogSettings
import settings.SecretString
import webhook.MainPushEventHandler
import webhook.MergeEventHandler
import webhook.PreviewPushEventHandler

class Module(@unused environment: Environment, configuration: Configuration)
    extends AbstractModule
    with PekkoGuiceSupport {

  override def configure(): Unit = {
    super.configure()

    bindSettings()

    bind(classOf[MetadataYamlPrinter]).toInstance(new MetadataYamlPrinter(2))
    bind(classOf[ModuleSubscribers])
      .toProvider(classOf[ModuleSubscribersProvider])
      .asEagerSingleton()
    bind(new TypeLiteral[Set[MetadataParser]] {}).toInstance(Set(new THKV1Parser()))

    bindActors()
  }

  private def bindActors(): Unit = {
    bindActor[ReviewNotificationActor]("ReviewNotificationActor")
    bindActor[MailActor]("MailActor")
    bindActor[PreviewPushEventHandler]("PreviewPushEventHandler")
    bindActor[MainPushEventHandler]("MainPushEventHandler")
    bindActor[PeopleImageUpdateActor]("PeopleImageUpdateActor")
    bindActor[ModulePublisher]("ModulePublisher")
    bindActor[ModuleDatabaseActor]("ModuleDatabaseActor")
    bindActor[MergeEventHandler]("MergeEventHandler")
  }

  private def bindSettings(): Unit = {
    val settings = AppSettings.load(configuration)
    bind(classOf[AppSettings]).toInstance(settings)
    bind(classOf[ExamListPathsSettings]).toInstance(
      ExamListPathsSettings(settings.play.tmpDir, settings.pandoc.examListOutputFolderPath)
    )
    bind(classOf[ModuleCatalogSettings]).toInstance(
      ModuleCatalogSettings(
        tmpDir = settings.play.tmpDir,
        publishedPdfDir = settings.pandoc.moduleCatalogOutputFolderPath,
        introDir = settings.pandoc.mcIntroPath,
        assetsDir = settings.pandoc.mcAssetsPath,
        texCommand = settings.pandoc.texCmd,
        wordCommand = settings.pandoc.wordCmd
      )
    )
    bind(classOf[MarkdownCLI]).toInstance(MarkdownCLI(settings.markdown))

    bind(classOf[GitCLI]).toInstance(
      GitCLI(Branch(settings.git.draftBranch), Paths.get(settings.git.localGitFolderPath))
    )
    bind(classOf[MailConfig]).toInstance(MailConfig(settings.mail.sender, 5))
    bind(classOf[KeycloakConfig]).toInstance(KeycloakConfig(settings.keycloak.jwksUrl, settings.keycloak.issuer))

    bind(classOf[GitConfig]).toInstance(
      GitConfig(
        SecretString.unwrap(settings.git.accessToken),
        settings.git.baseUrl,
        settings.git.projectId,
        Branch(settings.git.mainBranch),
        Branch(settings.git.draftBranch),
        settings.git.modulesFolder,
        settings.git.moduleCatalogsFolder,
        settings.git.moduleCompanionFolder,
        settings.git.autoApprovedLabel,
        settings.git.reviewRequiredLabel,
        settings.git.fastForwardLabel,
        settings.git.defaultEmail,
        settings.git.defaultUser,
        settings.git.historySince
      )
    )
    bind(classOf[ModuleKeysToReview]).toInstance(ModuleKeysToReview(settings.moduleKeysToReview.pavModuleKeys.toSet))
  }
}
