import scala.annotation.unused

import auth.KeycloakConfig
import cli.GitCliConfig
import cli.MarkdownConfig
import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import git.api.GitApiConfig
import git.api.GitCommitConfig
import git.publisher.ModulePublisher
import git.subscriber.ModuleDatabaseActor
import git.subscriber.ModuleSubscribers
import git.GitRepositoryConfig
import git.GitReviewConfig
import git.GitWebConfig
import models.ModuleKeysToReview
import parsing.metadata.MetadataParser
import parsing.metadata.THKV1Parser
import play.api.libs.concurrent.PekkoGuiceSupport
import play.api.Configuration
import play.api.Environment
import printing.yaml.MetadataYamlPrinter
import providers.ModuleSubscribersProvider
import service.artifact.modulecatalog.ModuleCatalogPdfConfig
import service.artifact.ExamListPdfConfig
import service.artifact.ExamLoadConfig
import service.history.ModuleHistoryConfig
import service.image.PeopleImageUpdateActor
import service.mail.MailActor
import service.mail.MailConfig
import service.notification.ReviewNotificationActor
import service.notification.ReviewNotificationConfig
import webhook.MainPushEventHandler
import webhook.MergeEventHandler
import webhook.PreviewPushEventHandler
import webhook.WebhookConfig

class Module(@unused environment: Environment, configuration: Configuration)
    extends AbstractModule
    with PekkoGuiceSupport {

  override def configure(): Unit = {
    super.configure()

    bindConfigs()

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

  private def bindConfigs(): Unit = {
    bind(classOf[WebhookConfig]).toInstance(WebhookConfig.load(configuration))
    bind(classOf[ExamLoadConfig]).toInstance(ExamLoadConfig.load(configuration))
    bind(classOf[ExamListPdfConfig]).toInstance(ExamListPdfConfig.load(configuration))
    bind(classOf[ModuleCatalogPdfConfig]).toInstance(ModuleCatalogPdfConfig.load(configuration))
    bind(classOf[MarkdownConfig]).toInstance(MarkdownConfig.load(configuration))
    bind(classOf[GitCliConfig]).toInstance(GitCliConfig.load(configuration))
    bind(classOf[MailConfig]).toInstance(MailConfig.load(configuration))
    bind(classOf[KeycloakConfig]).toInstance(KeycloakConfig.load(configuration))
    bind(classOf[ReviewNotificationConfig]).toInstance(ReviewNotificationConfig.load(configuration))
    bind(classOf[GitApiConfig]).toInstance(GitApiConfig.load(configuration))
    bind(classOf[GitRepositoryConfig]).toInstance(GitRepositoryConfig.load(configuration))
    bind(classOf[GitReviewConfig]).toInstance(GitReviewConfig.load(configuration))
    bind(classOf[GitCommitConfig]).toInstance(GitCommitConfig.load(configuration))
    bind(classOf[GitWebConfig]).toInstance(GitWebConfig.load(configuration))
    bind(classOf[ModuleHistoryConfig]).toInstance(ModuleHistoryConfig.load(configuration))
    bind(classOf[ModuleKeysToReview]).toInstance(ModuleKeysToReview.load(configuration))
  }
}
