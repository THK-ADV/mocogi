package providers

import akka.actor.ActorSystem
import catalog.ModuleCatalogService
import database.repo.{
  ModuleCatalogGenerationRequestRepository,
  ModuleDraftRepository,
  ModuleReviewRepository
}
import git.GitConfig
import git.api.{GitBranchService, GitMergeRequestApiService}
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
    moduleCatalogGenerationRepo: ModuleCatalogGenerationRequestRepository,
    mergeRequestApiService: GitMergeRequestApiService,
    branchService: GitBranchService,
    moduleCatalogService: ModuleCatalogService,
    configReader: ConfigReader,
    ctx: ExecutionContext
) extends Provider[GitMergeEventHandler] {
  override def get() = GitMergeEventHandler(
    system.actorOf(
      GitMergeEventHandler.props(
        gitConfig,
        moduleReviewRepository,
        moduleDraftRepository,
        moduleCatalogGenerationRepo,
        mergeRequestApiService,
        branchService,
        moduleCatalogService,
        configReader.bigBangLabel,
        configReader.moduleCatalogLabel,
        configReader.autoApprovedLabel,
        configReader.reviewRequiredLabel,
        15.seconds,
        10,
        ctx
      )
    )
  )
}
