package git.api

import git.{GitConfig, GitFileContent, GitFilePath}
import models.Branch
import play.api.libs.ws.WSClient

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitFileDownloadApiService @Inject() (
    private val ws: WSClient,
    val config: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {

  def download(
      path: GitFilePath,
      branch: Branch
  ): Future[Option[GitFileContent]] =
    ws
      .url(url(path, branch))
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

  private def urlEncoded(path: GitFilePath) =
    URLEncoder.encode(path.value, StandardCharsets.UTF_8)

  private def url(path: GitFilePath, branch: Branch) =
    s"${config.baseUrl}/projects/${config.projectId}/repository/files/${urlEncoded(path)}/raw?ref=${branch.value}"
}
