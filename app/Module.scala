import com.google.inject.AbstractModule
import parsing.metadata._
import parsing.{ModuleCompendiumParser, ModuleCompendiumParserImpl}
import printing.{ModuleCompendiumPrinter, ModuleCompendiumPrinterImpl}
import providers._

class Module extends AbstractModule {

  override def configure(): Unit = {
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
  }
}
