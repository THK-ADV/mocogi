package providers

import akka.actor.ActorSystem
import catalog.ModuleCatalogLatexActor
import catalog.ModuleCatalogLatexActor.{Config, GlabConfig}
import database.repo.core._
import database.repo.{ModuleCatalogRepository, ModuleRepository}
import database.view.StudyProgramViewRepository
import git.api.GitAvailabilityChecker
import printing.latex.ModuleCatalogLatexPrinter

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleCatalogLatexActorProvider @Inject() (
    system: ActorSystem,
    gitAvailabilityChecker: GitAvailabilityChecker,
    printer: ModuleCatalogLatexPrinter,
    moduleRepository: ModuleRepository,
    moduleCatalogRepository: ModuleCatalogRepository,
    studyProgramViewRepo: StudyProgramViewRepository,
    moduleTypeRepository: ModuleTypeRepository,
    languageRepository: LanguageRepository,
    seasonRepository: SeasonRepository,
    identityRepository: IdentityRepository,
    assessmentMethodRepository: AssessmentMethodRepository,
    config: ConfigReader,
    ctx: ExecutionContext
) extends Provider[ModuleCatalogLatexActor] {
  override def get() = new ModuleCatalogLatexActor(
    system.actorOf(
      ModuleCatalogLatexActor.props(
        gitAvailabilityChecker,
        printer,
        moduleRepository,
        moduleCatalogRepository,
        studyProgramViewRepo,
        moduleTypeRepository,
        languageRepository,
        seasonRepository,
        identityRepository,
        assessmentMethodRepository,
        Config(
          config.tmpFolderPath,
          config.moduleCatalogFolderPath,
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
