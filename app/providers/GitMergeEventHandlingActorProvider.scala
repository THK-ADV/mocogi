package providers

import git.GitConfig
import git.api.GitFileDownloadService
import git.publisher.{CoreDataPublisher, ModulePublisher}
import org.apache.pekko.actor.ActorSystem
import webhook.GitPushEventHandler

import javax.inject.{Inject, Provider}
import scala.concurrent.ExecutionContext

final class GitMergeEventHandlingActorProvider @Inject() (
    system: ActorSystem,
    downloadService: GitFileDownloadService,
    modulePublisher: ModulePublisher,
    coreDataPublisher: CoreDataPublisher,
    gitConfig: GitConfig,
    ctx: ExecutionContext
) extends Provider[GitPushEventHandler] {
  override def get() = GitPushEventHandler(
    system.actorOf(
      GitPushEventHandler.props(
        downloadService,
        modulePublisher,
        coreDataPublisher,
        gitConfig,
        ctx
      )
    )
  )
}
