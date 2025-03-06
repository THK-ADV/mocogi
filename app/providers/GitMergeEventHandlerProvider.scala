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
import database.repo.ModuleUpdatePermissionRepository
import git.api.GitBranchService
import git.api.GitCommitService
import git.api.GitMergeRequestApiService
import git.GitConfig
import ops.ConfigurationOps.Ops
import org.apache.pekko.actor.ActorSystem
import play.api.i18n.MessagesApi
import service.mail.MailerService
import service.ModuleCreationService
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
    gitCommitService: GitCommitService,
    moduleCreationService: ModuleCreationService,
    moduleUpdatePermissionRepository: ModuleUpdatePermissionRepository,
    mailerService: MailerService,
    messages: MessagesApi,
    ctx: ExecutionContext
) extends Provider[GitMergeEventHandler] {
  override def get() = GitMergeEventHandler(
    system.actorOf(
      GitMergeEventHandler.props(
        gitConfig,
        moduleReviewRepository,
        moduleDraftRepository,
        moduleCreationService,
        moduleCatalogGenerationRepo,
        mergeRequestApiService,
        branchService,
        gitCommitService,
        moduleCatalogService,
        moduleUpdatePermissionRepository,
        mailerService,
        messages,
        configReader.bigBangLabel,
        configReader.moduleCatalogLabel,
        configReader.autoApprovedLabel,
        configReader.reviewRequiredLabel,
        15.seconds,
        10,
        configReader.config.nonEmptyString("mail.editUrl"),
        ctx
      )
    )
  )
}
