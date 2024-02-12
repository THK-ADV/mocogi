package git.api

import git.{GitConfig, GitFileContent, GitFilePath}
import models.Branch
import play.api.libs.ws.WSClient

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitFileApiService @Inject() (
    private val ws: WSClient,
    val config: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {

  def download(
      path: GitFilePath,
      branch: Branch
  ): Future[Option[GitFileContent]] =
    ws
      .url(fileUrl(path, branch))
      .addHttpHeaders(tokenHeader())
      .get()
      .flatMap { r =>
        r.status match {
          case 200 =>
            Future.successful(Some(GitFileContent(r.bodyAsBytes.utf8String)))
          case 404 =>
            Future.successful(None)
          case _ =>
            Future.failed(
              new Throwable(
                r.json
                  .\("message")
                  .validate[String]
                  .getOrElse("unknown response message")
              )
            )
        }
      }

  def fileExists(path: GitFilePath, branch: Branch) =
    ws
      .url(fileUrl(path, branch))
      .addHttpHeaders(tokenHeader())
      .head()
      .flatMap { r =>
        r.status match {
          case 200 => Future.successful(true)
          case 404 => Future.successful(false)
          case _   => Future.failed(parseErrorMessage(r))
        }
      }
}
