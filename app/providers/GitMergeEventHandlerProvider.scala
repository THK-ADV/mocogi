package providers

import akka.actor.ActorSystem
import catalog.ModuleCatalogLatexActor
import catalog.ModuleCatalogLatexActor.Config
import database.repo.core._
import database.repo.{
  ModuleCatalogGenerationRequestRepository,
  ModuleCatalogRepository,
  ModuleDraftRepository,
  ModuleRepository,
  ModuleReviewRepository
}
import database.view.StudyProgramViewRepository
import git.GitConfig
import git.api.{
  GitAvailabilityChecker,
  GitBranchService,
  GitMergeRequestApiService
}
import models.Branch
import printing.latex.ModuleCatalogLatexPrinter
import webhook.GitMergeEventHandler

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
final class GitMergeEventHandlerProvider @Inject() (
    system: ActorSystem,
    gitConfig: GitConfig,
    moduleReviewRepository: ModuleReviewRepository,
    moduleDraftRepository: ModuleDraftRepository,
    mergeRequestApiService: GitMergeRequestApiService,
    moduleCatalogGenerationRepo: ModuleCatalogGenerationRequestRepository,
    configReader: ConfigReader,
    gitAvailabilityChecker: GitAvailabilityChecker,
    printer: ModuleCatalogLatexPrinter,
    moduleRepository: ModuleRepository,
    catalogRepository: ModuleCatalogRepository,
    studyProgramViewRepo: StudyProgramViewRepository,
    moduleTypeRepository: ModuleTypeRepository,
    languageRepository: LanguageRepository,
    seasonRepository: SeasonRepository,
    identityRepository: IdentityRepository,
    assessmentMethodRepository: AssessmentMethodRepository,
    branchService: GitBranchService,
    ctx: ExecutionContext
) extends Provider[GitMergeEventHandler] {
  override def get() = GitMergeEventHandler(
    system.actorOf(
      GitMergeEventHandler.props(
        gitConfig,
        moduleReviewRepository,
        moduleDraftRepository,
        mergeRequestApiService,
        branchService,
        configReader.bigBangLabel,
        configReader.moduleCatalogLabel,
        configReader.autoApprovedLabel,
        configReader.reviewRequiredLabel,
        10.seconds,
        system.actorOf(
          ModuleCatalogLatexActor.props(
            gitAvailabilityChecker,
            printer,
            moduleRepository,
            catalogRepository,
            studyProgramViewRepo,
            moduleTypeRepository,
            languageRepository,
            seasonRepository,
            identityRepository,
            assessmentMethodRepository,
            moduleCatalogGenerationRepo,
            mergeRequestApiService,
            Config(
              configReader.tmpFolderPath,
              configReader.moduleCatalogFolderPath,
              configReader.repoPath,
              configReader.mcPath,
              configReader.pushScriptPath,
              Branch(configReader.mainBranch),
              configReader.moduleCatalogLabel
            ),
            ctx
          )
        ),
        moduleCatalogGenerationRepo,
        10,
        ctx
      )
    )
  )
}
