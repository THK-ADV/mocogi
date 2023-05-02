package git.publisher

import akka.actor.{Actor, ActorRef, Props}
import git._
import git.publisher.GitFilesDownloadActor.Download
import play.api.Logging
import play.api.libs.ws.WSClient

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object GitFilesDownloadActor {
  def props(
      gitConfig: GitConfig,
      broker: GitFilesBroker,
      ws: WSClient,
      ctx: ExecutionContext
  ) = Props(
    new GitFilesDownloadActorImpl(gitConfig, broker, ws, ctx)
  )

  private case class Download(
      changes: GitChanges[List[GitFilePath]],
      projectId: Int
  )

  private final class GitFilesDownloadActorImpl(
      private val gitConfig: GitConfig,
      private val broker: GitFilesBroker,
      private val ws: WSClient,
      private implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {

    override def receive = { case Download(changes, projectId) =>
      val urls = stitchFileUrl(projectId, changes)
      downloadFiles(urls) onComplete {
        case Success(s) =>
          broker.distributeToSubscriber(s)
        case Failure(t) =>
          logger.error( // TODO pull out
            s"""failed to download git file
               |  - message: ${t.getMessage}
               |  - trace: ${t.getStackTrace.mkString(
                "\n           "
              )}""".stripMargin
          )
      }
    }

    private def downloadFile(
        t: (GitFilePath, GitFileURL)
    ): Future[(GitFilePath, GitFileContent)] =
      ws
        .url(t._2.value)
        .addHttpHeaders(accessTokenHeader())
        .get()
        .flatMap { r =>
          if (r.status == 200)
            Future.successful(t._1 -> GitFileContent(r.bodyAsBytes.utf8String))
          else
            Future.failed(
              new Throwable(
                r.json
                  .\("message")
                  .validate[String]
                  .getOrElse("unknown response message")
              )
            )
        }

    private def downloadFiles(
        changes: GitChanges[List[(GitFilePath, GitFileURL)]]
    ): Future[GitChanges[List[(GitFilePath, GitFileContent)]]] =
      for {
        added <- Future.sequence(changes.added.map(downloadFile))
        modified <- Future.sequence(changes.modified.map(downloadFile))
      } yield changes.copy(added, modified)

    private def accessTokenHeader(): (String, String) =
      "PRIVATE-TOKEN" -> gitConfig.accessToken

    private def stitchFileUrl(
        projectId: Int,
        changes: GitChanges[List[GitFilePath]]
    ): GitChanges[List[(GitFilePath, GitFileURL)]] = {
      def urlEncoded(path: GitFilePath) =
        URLEncoder.encode(path.value, StandardCharsets.UTF_8)
      def go(path: GitFilePath): GitFileURL =
        GitFileURL(
          s"${gitConfig.baseUrl}/projects/$projectId/repository/files/${urlEncoded(path)}/raw?ref=master"
        )
      changes.copy(
        changes.added.map(p => p -> go(p)),
        changes.modified.map(p => p -> go(p))
      )
    }
  }
}

@Singleton
case class GitFilesDownloadActor(private val value: ActorRef) {
  def download(changes: GitChanges[List[GitFilePath]], projectId: Int): Unit =
    value ! Download(changes: GitChanges[List[GitFilePath]], projectId: Int)
}
