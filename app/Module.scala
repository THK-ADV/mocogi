import com.google.inject.{AbstractModule, TypeLiteral}
import git.publisher.{GitFilesDownloadActor, ModuleCompendiumPublisher}
import git.{GitConfig, ModuleCompendiumSubscribers}
import parsing.metadata.{MetadataParser, _}
import printing.{MarkdownConverter, ModuleCompendiumPrinter, ModuleCompendiumPrinterImpl}
import providers._
import service._

class Module() extends AbstractModule {

  override def configure(): Unit = {
    super.configure()

    bind(classOf[ModuleCompendiumPrinter])
      .to(classOf[ModuleCompendiumPrinterImpl])
      .asEagerSingleton()

    bind(classOf[LocationService])
      .to(classOf[LocationServiceImpl])
      .asEagerSingleton()
    bind(classOf[LanguageService])
      .to(classOf[LanguageServiceImpl])
      .asEagerSingleton()
    bind(classOf[StatusService])
      .to(classOf[StatusServiceImpl])
      .asEagerSingleton()
    bind(classOf[AssessmentMethodService])
      .to(classOf[AssessmentMethodServiceImpl])
      .asEagerSingleton()
    bind(classOf[ModuleTypeService])
      .to(classOf[ModuleTypeServiceImpl])
      .asEagerSingleton()
    bind(classOf[SeasonService])
      .to(classOf[SeasonServiceImpl])
      .asEagerSingleton()
    bind(classOf[PersonService])
      .to(classOf[PersonServiceImpl])
      .asEagerSingleton()

    bind(classOf[MarkdownConverter])
      .toProvider(classOf[MarkdownConverterProvider])
      .asEagerSingleton()
    bind(classOf[GitConfig])
      .toProvider(classOf[GitConfigProvider])
      .asEagerSingleton()
    bind(classOf[ModuleCompendiumSubscribers])
      .toProvider(classOf[ModuleCompendiumSubscribersProvider])
      .asEagerSingleton()
    bind(classOf[GitFilesDownloadActor])
      .toProvider(classOf[GitFilesDownloadActorProvider])
      .asEagerSingleton()
    bind(classOf[ModuleCompendiumPublisher])
      .toProvider(classOf[ModuleCompendiumPublisherProvider])
      .asEagerSingleton()

    bind(new TypeLiteral[Set[MetadataParser]] {})
      .toProvider(classOf[MetadataParserProvider])
      .asEagerSingleton()
  }
}
