import com.google.inject.AbstractModule
import git.GitToken
import parsing.metadata._
import parsing.{ModuleCompendiumParser, ModuleCompendiumParserImpl}
import play.api.Configuration
import printing.{
  MarkdownConverter,
  ModuleCompendiumPrinter,
  ModuleCompendiumPrinterImpl
}
import providers._

import java.util.UUID
import scala.util.Try

class Module(config: Configuration) extends AbstractModule {

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

    bind(classOf[GitToken])
      .toInstance(GitToken(gitToken))
  }

  def gitToken: Option[UUID] =
    config
      .getOptional[String]("git.token")
      .flatMap(s => Try(UUID.fromString(s)).toOption)
}
