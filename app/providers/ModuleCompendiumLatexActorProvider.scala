package providers

import akka.actor.ActorSystem
import compendium.ModuleCompendiumLatexActor
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
import git.api.GitAvailabilityChecker
import printing.latex.ModuleCompendiumLatexPrinter
import ModuleCompendiumLatexActor.{Config, GlabConfig}

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleCompendiumLatexActorProvider @Inject() (
    system: ActorSystem,
    gitAvailabilityChecker: GitAvailabilityChecker,
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
        gitAvailabilityChecker,
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
          config.moduleCompendiumFolderPath,
          GlabConfig(
            config.repoPath,
            config.mcPath,
            config.pushScriptPath,
            config.mainBranch
          )
        ),
        ctx
      )
    )
  )
}
