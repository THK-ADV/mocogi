package git.api

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.*
import play.api.libs.ws.WSClient

@Singleton
final class GitFileApiService @Inject() (
    private val ws: WSClient,
    val config: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {

  def download(
      path: GitFilePath,
      branch: Branch
  ): Future[Option[(GitFileContent, Option[CommitId])]] =
    ws
      .url(fileUrlRaw(path, branch))
      .addHttpHeaders(tokenHeader())
      .get()
      .flatMap { r =>
        r.status match {
          case 200 =>
            Future.successful(
              Some(GitFileContent(r.bodyAsBytes.utf8String), r.header("X-Gitlab-Last-Commit-Id").map(CommitId.apply))
            )
          case 404 =>
            Future.successful(None)
          case _ =>
            Future.failed(
              new Exception(
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
      .url(fileUrlRaw(path, branch))
      .addHttpHeaders(tokenHeader())
      .head()
      .flatMap { r =>
        r.status match {
          case 200 => Future.successful(true)
          case 404 => Future.successful(false)
          case _   => Future.failed(parseErrorMessage(r))
        }
      }

  private def urlEncoded(path: GitFilePath) =
    URLEncoder.encode(path.value, StandardCharsets.UTF_8)

  private def fileUrlRaw(path: GitFilePath, branch: Branch) =
    s"${config.baseUrl}/projects/${config.projectId}/repository/files/${urlEncoded(path)}/raw?ref=${branch.value}"
}
