package providers

import akka.actor.ActorSystem
import git.GitConfig
import git.api.GitFileDownloadService
import git.publisher.{CoreDataPublisher, ModuleCompendiumPublisher}
import git.webhook.GitPushEventHandlingActor
import service.ModuleDraftService

import javax.inject.{Inject, Provider}
import scala.concurrent.ExecutionContext

final class GitMergeEventHandlingActorProvider @Inject() (
    system: ActorSystem,
    moduleDraftService: ModuleDraftService,
    downloadService: GitFileDownloadService,
    moduleCompendiumPublisher: ModuleCompendiumPublisher,
    coreDataPublisher: CoreDataPublisher,
    gitConfig: GitConfig,
    ctx: ExecutionContext
) extends Provider[GitPushEventHandlingActor] {
  override def get() = GitPushEventHandlingActor(
    system.actorOf(
      GitPushEventHandlingActor.props(
        moduleDraftService,
        downloadService,
        moduleCompendiumPublisher,
        coreDataPublisher,
        gitConfig,
        ctx
      )
    )
  )
}
