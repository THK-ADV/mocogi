package providers

import akka.actor.ActorSystem
import database.repo.{
  AssessmentMethodRepository,
  LanguageRepository,
  ModuleCompendiumListRepository,
  ModuleCompendiumRepository,
  ModuleTypeRepository,
  PORepository,
  PersonRepository,
  SeasonRepository
}
import printing.latex.ModuleCompendiumLatexPrinter
import service.ModuleCompendiumLatexActor
import service.ModuleCompendiumLatexActor.Config

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleCompendiumLatexActorProvider @Inject() (
    system: ActorSystem,
    printer: ModuleCompendiumLatexPrinter,
    moduleCompendiumRepository: ModuleCompendiumRepository,
    moduleCompendiumListRepository: ModuleCompendiumListRepository,
    poRepository: PORepository,
    moduleTypeRepository: ModuleTypeRepository,
    languageRepository: LanguageRepository,
    seasonRepository: SeasonRepository,
    personRepository: PersonRepository,
    assessmentMethodRepository: AssessmentMethodRepository,
    config: ConfigReader,
    ctx: ExecutionContext
) extends Provider[ModuleCompendiumLatexActor] {
  override def get() = new ModuleCompendiumLatexActor(
    system.actorOf(
      ModuleCompendiumLatexActor.props(
        printer,
        moduleCompendiumRepository,
        moduleCompendiumListRepository,
        poRepository,
        moduleTypeRepository,
        languageRepository,
        seasonRepository,
        personRepository,
        assessmentMethodRepository,
        Config(
          config.textBinPath,
          config.compileScriptPath,
          config.clearScriptPath,
          config.tmpFolderPath,
          config.publicFolderName,
          config.assetsFolderName
        ),
        ctx
      )
    )
  )
}
