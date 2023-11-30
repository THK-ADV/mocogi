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
import service.ModuleCompendiumLatexActor.{Config, GlabConfig}

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
          config.tmpFolderPath,
          config.outputFolderPath,
          config.repoPath
            .zip(config.mcPath)
            .zip(config.pushScriptPath)
            .map(a => GlabConfig(a._1._1, a._1._2, a._2, config.mainBranch))
        ),
        ctx
      )
    )
  )
}
