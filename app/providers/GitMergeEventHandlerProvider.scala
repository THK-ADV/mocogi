package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext

import catalog.ModuleCatalogService
import database.repo.ModuleCatalogGenerationRequestRepository
import database.repo.ModuleDraftRepository
import database.repo.ModuleReviewRepository
import git.api.GitBranchService
import git.api.GitMergeRequestApiService
import git.GitConfig
import org.apache.pekko.actor.ActorSystem
import webhook.GitMergeEventHandler

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
