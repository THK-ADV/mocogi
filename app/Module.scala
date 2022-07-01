import com.google.inject.AbstractModule
import git.{
  GitConfig,
  ModuleCompendiumPublisher,
  ModuleCompendiumPublisherImpl,
  ModuleCompendiumSubscribers
}
import parsing.metadata._
import parsing.{ModuleCompendiumParser, ModuleCompendiumParserImpl}
import printing.{
  MarkdownConverter,
  ModuleCompendiumPrinter,
  ModuleCompendiumPrinterImpl
}
import providers._

class Module() extends AbstractModule {

  override def configure(): Unit = {
    super.configure()

    bind(classOf[ModuleCompendiumParser])
      .to(classOf[ModuleCompendiumParserImpl])
      .asEagerSingleton()
    bind(classOf[ModuleCompendiumPrinter])
      .to(classOf[ModuleCompendiumPrinterImpl])
      .asEagerSingleton()
    bind(classOf[MetadataParser])
      .to(classOf[MetadataParserImpl])
      .asEagerSingleton()
    bind(classOf[ResponsibilitiesParser])
      .to(classOf[ResponsibilitiesParserImpl])
      .asEagerSingleton()
    bind(classOf[ModuleCompendiumPublisher])
      .to(classOf[ModuleCompendiumPublisherImpl])
      .asEagerSingleton()

    bind(classOf[SeasonParser])
      .toProvider(classOf[SeasonParserProvider])
      .asEagerSingleton()
    bind(classOf[PeopleParser])
      .toProvider(classOf[PeopleParserProvider])
      .asEagerSingleton()
    bind(classOf[AssessmentMethodParser])
      .toProvider(classOf[AssessmentMethodParserProvider])
      .asEagerSingleton()
    bind(classOf[StatusParser])
      .toProvider(classOf[StatusParserProvider])
      .asEagerSingleton()
    bind(classOf[ModuleTypeParser])
      .toProvider(classOf[ModuleTypeParserProvider])
      .asEagerSingleton()
    bind(classOf[LocationParser])
      .toProvider(classOf[LocationParserProvider])
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
  }
}
