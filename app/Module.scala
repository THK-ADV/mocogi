import scala.annotation.unused

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import git.publisher.CoreDataPublisher
import git.publisher.ModulePublisher
import git.subscriber.ModuleDatabaseActor
import git.subscriber.ModuleSubscribers
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
import settings.GitCliGuiceProvider
import settings.GitConfigProvider
import settings.KeycloakConfigGuiceProvider
import settings.MailConfigGuiceProvider
import settings.ModuleKeysToReviewProvider
import webhook.MainPushEventHandler
import webhook.MergeEventHandler
import webhook.PreviewPushEventHandler
import auth.KeycloakConfig
import cli.GitCLI

class Module(@unused environment: Environment, @unused configuration: Configuration)
    extends AbstractModule
    with PekkoGuiceSupport {

  override def configure(): Unit = {
    super.configure()

    bind(classOf[AppSettings]).toInstance(AppSettings.load(configuration))

    bind(classOf[GitCLI]).toProvider(classOf[GitCliGuiceProvider])
    bind(classOf[MailConfig]).toProvider(classOf[MailConfigGuiceProvider])
    bind(classOf[KeycloakConfig]).toProvider(classOf[KeycloakConfigGuiceProvider])

    bind(classOf[MetadataYamlPrinter]).toInstance(new MetadataYamlPrinter(2))

    bind(classOf[GitConfig])
      .toProvider(classOf[GitConfigProvider])
      .asEagerSingleton()
    bind(classOf[ModuleSubscribers])
      .toProvider(classOf[ModuleSubscribersProvider])
      .asEagerSingleton()
    bind(classOf[ModuleKeysToReview])
      .toProvider(classOf[ModuleKeysToReviewProvider])
      .asEagerSingleton()
    bind(new TypeLiteral[Set[MetadataParser]] {}).toInstance(Set(new THKV1Parser()))

    bindActor[ReviewNotificationActor]("ReviewNotificationActor")
    bindActor[MailActor]("MailActor")
    bindActor[PreviewPushEventHandler]("PreviewPushEventHandler")
    bindActor[MainPushEventHandler]("MainPushEventHandler")
    bindActor[PeopleImageUpdateActor]("PeopleImageUpdateActor")
    bindActor[CoreDataPublisher]("CoreDataPublisher")
    bindActor[ModulePublisher]("ModulePublisher")
    bindActor[ModuleDatabaseActor]("ModuleDatabaseActor")
    bindActor[MergeEventHandler]("MergeEventHandler")
  }
}
