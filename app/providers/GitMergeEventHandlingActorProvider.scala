package providers

import akka.actor.ActorSystem
import database.repo.UserBranchRepository
import git.GitConfig
import git.subscriber.ModuleCompendiumSubscribers
import git.webhook.GitMergeEventHandlingActor
import service.ModuleDraftService

import javax.inject.{Inject, Provider}
import scala.concurrent.ExecutionContext

final class GitMergeEventHandlingActorProvider @Inject() (
    system: ActorSystem,
    userBranchRepository: UserBranchRepository,
    moduleDraftService: ModuleDraftService,
    subscribers: ModuleCompendiumSubscribers,
    gitConfig: GitConfig,
    ctx: ExecutionContext
) extends Provider[GitMergeEventHandlingActor] {
  override def get() = GitMergeEventHandlingActor(
    system.actorOf(
      GitMergeEventHandlingActor.props(
        userBranchRepository,
        moduleDraftService,
        subscribers,
        gitConfig,
        ctx
      )
    )
  )
}
