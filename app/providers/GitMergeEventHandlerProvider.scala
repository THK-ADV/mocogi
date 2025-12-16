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
import git.api.GitFileApiService
import git.api.GitMergeRequestApiService
import git.GitConfig
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.ActorSystem
import play.api.i18n.MessagesApi
import service.MetadataPipeline
import service.ModuleCreationService
import webhook.MergeEventHandler

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
    modulePipeline: MetadataPipeline,
    gitFileApiService: GitFileApiService,
    ctx: ExecutionContext
) extends Provider[MergeEventHandler] {
  override def get() = MergeEventHandler(
    system.actorOf(
      MergeEventHandler.props(
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
        modulePipeline,
        gitFileApiService,
        ctx
      )
    )
  )
}
