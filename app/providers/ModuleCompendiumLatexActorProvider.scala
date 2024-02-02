package providers

import akka.actor.ActorSystem
import compendium.ModuleCompendiumLatexActor
import compendium.ModuleCompendiumLatexActor.{Config, GlabConfig}
import database.repo.{
  AssessmentMethodRepository,
  IdentityRepository,
  LanguageRepository,
  ModuleCompendiumListRepository,
  ModuleCompendiumRepository,
  ModuleTypeRepository,
  SeasonRepository
}
import database.view.StudyProgramViewRepository
import git.api.GitAvailabilityChecker
import printing.latex.ModuleCompendiumLatexPrinter

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleCompendiumLatexActorProvider @Inject() (
    system: ActorSystem,
    gitAvailabilityChecker: GitAvailabilityChecker,
    printer: ModuleCompendiumLatexPrinter,
    moduleCompendiumRepository: ModuleCompendiumRepository,
    moduleCompendiumListRepository: ModuleCompendiumListRepository,
    studyProgramViewRepo: StudyProgramViewRepository,
    moduleTypeRepository: ModuleTypeRepository,
    languageRepository: LanguageRepository,
    seasonRepository: SeasonRepository,
    identityRepository: IdentityRepository,
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
        studyProgramViewRepo,
        moduleTypeRepository,
        languageRepository,
        seasonRepository,
        identityRepository,
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
