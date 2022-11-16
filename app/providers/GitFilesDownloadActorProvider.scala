package providers

import akka.actor.ActorSystem
import git.publisher.GitFilesDownloadActor
import git.{GitConfig, GitFilesBroker}
import play.api.libs.ws.WSClient

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class GitFilesDownloadActorProvider @Inject() (
    system: ActorSystem,
    gitConfig: GitConfig,
    broker: GitFilesBroker,
    ws: WSClient,
    ctx: ExecutionContext
) extends Provider[GitFilesDownloadActor] {
  override def get(): GitFilesDownloadActor =
    GitFilesDownloadActor(
      system.actorOf(
        GitFilesDownloadActor.props(
          gitConfig,
          broker,
          ws,
          ctx
        )
      )
    )

}
