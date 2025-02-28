package providers

import javax.inject.Inject
import javax.inject.Provider

import scala.concurrent.ExecutionContext

import git.api.GitCommitApiService
import git.api.GitFileDownloadService
import git.publisher.CoreDataPublisher
import git.publisher.ModulePublisher
import git.GitConfig
import org.apache.pekko.actor.ActorSystem
import webhook.GitPushEventHandler

final class GitMergeEventHandlingActorProvider @Inject() (
    system: ActorSystem,
    downloadService: GitFileDownloadService,
    modulePublisher: ModulePublisher,
    coreDataPublisher: CoreDataPublisher,
    commitApiService: GitCommitApiService,
    gitConfig: GitConfig,
    ctx: ExecutionContext
) extends Provider[GitPushEventHandler] {
  override def get() = GitPushEventHandler(
    system.actorOf(
      GitPushEventHandler.props(
        downloadService,
        commitApiService,
        modulePublisher,
        coreDataPublisher,
        gitConfig,
        ctx
      )
    )
  )
}
