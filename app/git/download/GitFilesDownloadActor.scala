package git.download

import akka.actor.{ActorRef, Props}
import git.publisher.ModuleCompendiumPublisher
import git.{GitChanges, GitConfig, GitFilePath}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

object GitFilesDownloadActor {
  def props(
      gitConfig: GitConfig,
      publisher: ModuleCompendiumPublisher,
      ws: WSClient,
      ctx: ExecutionContext
  ) = Props(
    new GitFilesDownloadActorImpl(gitConfig, publisher, ws, ctx)
  )

  case class Download(changes: GitChanges[List[GitFilePath]], projectId: Int)
}

case class GitFilesDownloadActor(value: ActorRef)
