package providers

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.ModuleDraftRepository
import database.repo.ModuleReviewRepository
import database.repo.ModuleUpdatePermissionRepository
import git.api.GitCommitService
import git.api.GitMergeRequestApiService
import git.GitConfig
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.ActorSystem
import play.api.i18n.MessagesApi
import service.ModuleCreationService
import webhook.GitMergeEventHandler

@Singleton
final class GitMergeEventHandlerProvider @Inject() (
    system: ActorSystem,
    gitConfig: GitConfig,
    moduleReviewRepository: ModuleReviewRepository,
    moduleDraftRepository: ModuleDraftRepository,
    mergeRequestApiService: GitMergeRequestApiService,
    configReader: ConfigReader,
    gitCommitService: GitCommitService,
    moduleCreationService: ModuleCreationService,
    moduleUpdatePermissionRepository: ModuleUpdatePermissionRepository,
    @Named("MailActor") mailActor: ActorRef,
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
        mergeRequestApiService,
        gitCommitService,
        moduleUpdatePermissionRepository,
        mailActor,
        messages,
        configReader.autoApprovedLabel,
        configReader.reviewRequiredLabel,
        configReader.moduleEditUrl,
        10,
        ctx
      )
    )
  )
}
