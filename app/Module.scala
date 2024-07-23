import auth.{Authorization, UserToken}
import catalog.{ElectivesCatalogService, ModuleCatalogConfig, PreviewMergeActor}
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, TypeLiteral}
import git.GitConfig
import git.publisher.{CoreDataPublisher, ModulePublisher}
import git.subscriber.ModuleSubscribers
import models.ModuleKeysToReview
import ops.ConfigurationOps.Ops
import parsing.metadata.MetadataParser
import play.api.{Configuration, Environment}
import printing.markdown.ModuleMarkdownPrinter
import printing.pandoc.PandocApi
import printing.yaml.MetadataYamlPrinter
import providers._
import service.core._
import webhook.{GitMergeEventHandler, GitPushEventHandler}

import scala.annotation.unused

class Module(@unused environment: Environment, configuration: Configuration)
    extends AbstractModule {

  override def configure(): Unit = {
    super.configure()

    bind(classOf[PandocApi])
      .toProvider(classOf[MarkdownConverterProvider])
      .asEagerSingleton()
    bind(classOf[GitConfig])
      .toProvider(classOf[GitConfigProvider])
      .asEagerSingleton()
    bind(classOf[ModuleSubscribers])
      .toProvider(classOf[ModuleSubscribersProvider])
      .asEagerSingleton()
    bind(classOf[GitPushEventHandler])
      .toProvider(classOf[GitMergeEventHandlingActorProvider])
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
    bind(classOf[ModuleKeyService])
      .toProvider(classOf[ModuleKeyServiceProvider])
      .asEagerSingleton()
    bind(classOf[PreviewMergeActor])
      .toProvider(classOf[PreviewMergeActorProvider])
      .asEagerSingleton()
    bind(classOf[GitMergeEventHandler])
      .toProvider(classOf[GitMergeEventHandlerProvider])
      .asEagerSingleton()
    bind(classOf[ModuleCatalogConfig])
      .toProvider(classOf[ModuleCatalogConfigProvider])
      .asEagerSingleton()
    bind(classOf[ElectivesCatalogService])
      .toProvider(classOf[ElectivesCatalogServiceProvider])
      .asEagerSingleton()

    bind(new TypeLiteral[Set[MetadataParser]] {})
      .toProvider(classOf[MetadataParserProvider])
      .asEagerSingleton()
    bind(new TypeLiteral[Authorization[UserToken]] {})
      .toProvider(classOf[AuthorizationProvider])
      .asEagerSingleton()

    bind(classOf[MetadataYamlPrinter]).toInstance(
      new MetadataYamlPrinter(2)
    )
    bind(classOf[ModuleMarkdownPrinter]).toInstance(
      new ModuleMarkdownPrinter(true)
    )

    bind(classOf[String])
      .annotatedWith(Names.named("gitHost"))
      .toInstance(configuration.nonEmptyString("git.host"))
  }
}
