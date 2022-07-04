package providers

import akka.actor.ActorSystem
import git.publisher.ModuleCompendiumPublisher
import git.GitConfig
import git.download.GitFilesDownloadActor
import play.api.libs.ws.WSClient

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class GitFilesDownloadActorProvider @Inject() (
    system: ActorSystem,
    gitConfig: GitConfig,
    publisher: ModuleCompendiumPublisher,
    ws: WSClient,
    ctx: ExecutionContext
) extends Provider[GitFilesDownloadActor] {
  override def get(): GitFilesDownloadActor =
    GitFilesDownloadActor(
      system.actorOf(
        GitFilesDownloadActor.props(
          gitConfig,
          publisher,
          ws,
          ctx
        )
      )
    )

}
