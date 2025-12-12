import java.nio.file.Paths

import scala.annotation.unused

import auth.Authorization
import auth.Token
import com.google.inject.name.Names
import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import git.cli.ModuleGitCLI
import git.publisher.CoreDataPublisher
import git.publisher.ModulePublisher
import git.subscriber.ModuleSubscribers
import git.Branch
import git.GitConfig
import models.ModuleKeysToReview
import ops.ConfigurationOps.Ops
import parsing.metadata.MetadataParser
import play.api.libs.concurrent.PekkoGuiceSupport
import play.api.Configuration
import play.api.Environment
import printing.yaml.MetadataYamlPrinter
import providers.*
import service.image.PeopleImageUpdateActor
import service.mail.MailActor
import service.mail.MailConfig
import service.notification.ReviewNotificationActor
import webhook.MainPushEventHandler
import webhook.MergeEventHandler
import webhook.PreviewPushEventHandler

class Module(@unused environment: Environment, configuration: Configuration)
    extends AbstractModule
    with PekkoGuiceSupport {

  override def configure(): Unit = {
    super.configure()

    bind(classOf[ModuleGitCLI])
      .toInstance(
        new ModuleGitCLI(
          Branch(configuration.nonEmptyString("git.draftBranch")),
          Paths.get(configuration.nonEmptyString("git.localGitFolderPath"))
        )
      )

    bind(classOf[MetadataYamlPrinter]).toInstance(new MetadataYamlPrinter(2))

    bind(classOf[MailConfig]).toInstance(MailConfig(configuration.nonEmptyString("mail.sender"), 5))

    bind(classOf[String])
      .annotatedWith(Names.named("git.repoUrl"))
      .toInstance(configuration.nonEmptyString("git.repoUrl"))

    bind(classOf[String])
      .annotatedWith(Names.named("tmp.dir"))
      .toInstance(configuration.nonEmptyString("play.temporaryFile.dir"))

    bind(classOf[String])
      .annotatedWith(Names.named("cmd.word"))
      .toInstance(configuration.nonEmptyString("pandoc.wordCmd"))

    bind(classOf[String])
      .annotatedWith(Names.named("cmd.tex"))
      .toInstance(configuration.nonEmptyString("pandoc.texCmd"))

    bind(classOf[String])
      .annotatedWith(Names.named("path.mcIntro"))
      .toInstance(configuration.nonEmptyString("pandoc.mcIntroPath"))

    bind(classOf[String])
      .annotatedWith(Names.named("path.mcAssets"))
      .toInstance(configuration.nonEmptyString("pandoc.mcAssetsPath"))

    bind(classOf[Boolean])
      .annotatedWith(Names.named("substituteLocalisedContent"))
      .toInstance(true)

    bind(classOf[String])
      .annotatedWith(Names.named("reviewNotificationUrl"))
      .toInstance(configuration.nonEmptyString("mail.reviewUrl"))

    bind(classOf[String])
      .annotatedWith(Names.named("examListFolder"))
      .toInstance(configuration.nonEmptyString("pandoc.examListOutputFolderPath"))

    bind(classOf[GitConfig])
      .toProvider(classOf[GitConfigProvider])
      .asEagerSingleton()
    bind(classOf[ModuleSubscribers])
      .toProvider(classOf[ModuleSubscribersProvider])
      .asEagerSingleton()
    bind(classOf[CoreDataPublisher])
      .toProvider(classOf[CoreDataPublisherProvider])
      .asEagerSingleton()
    bind(classOf[ModulePublisher])
      .toProvider(classOf[ModulePublisherProvider])
      .asEagerSingleton()
    bind(classOf[ModuleKeysToReview])
      .toProvider(classOf[ModuleKeysToReviewProvider])
      .asEagerSingleton()
    bind(classOf[MergeEventHandler])
      .toProvider(classOf[GitMergeEventHandlerProvider])
      .asEagerSingleton()
    bind(new TypeLiteral[Set[MetadataParser]] {})
      .toProvider(classOf[MetadataParserProvider])
      .asEagerSingleton()
    bind(new TypeLiteral[Authorization[Token]] {})
      .toProvider(classOf[AuthorizationProvider])
      .asEagerSingleton()

    bindActor[ReviewNotificationActor]("ReviewNotificationActor")
    bindActor[MailActor]("MailActor")
    bindActor[PreviewPushEventHandler]("PreviewPushEventHandler")
    bindActor[MainPushEventHandler]("MainPushEventHandler")
    bindActor[PeopleImageUpdateActor]("PeopleImageUpdateActor")
  }
}
