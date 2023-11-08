package providers

import akka.actor.ActorSystem
import database.repo.{ModuleDraftRepository, ModuleReviewRepository}
import git.GitConfig
import git.api.GitFileDownloadService
import git.publisher.{CoreDataPublisher, ModuleCompendiumPublisher}
import git.webhook.GitPushEventHandlingActor

import javax.inject.{Inject, Provider}
import scala.concurrent.ExecutionContext

final class GitMergeEventHandlingActorProvider @Inject() (
    system: ActorSystem,
    downloadService: GitFileDownloadService,
    moduleCompendiumPublisher: ModuleCompendiumPublisher,
    coreDataPublisher: CoreDataPublisher,
    moduleReviewRepository: ModuleReviewRepository,
    moduleDraftRepository: ModuleDraftRepository,
    gitConfig: GitConfig,
    ctx: ExecutionContext
) extends Provider[GitPushEventHandlingActor] {
  override def get() = GitPushEventHandlingActor(
    system.actorOf(
      GitPushEventHandlingActor.props(
        downloadService,
        moduleCompendiumPublisher,
        coreDataPublisher,
        moduleReviewRepository,
        moduleDraftRepository,
        gitConfig,
        ctx
      )
    )
  )
}
