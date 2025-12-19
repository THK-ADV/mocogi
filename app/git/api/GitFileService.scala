package git.api

import java.util.UUID
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.Inject
import git.*
import models.ModuleProtocol
import ops.toFuture
import parsing.RawModuleParser
import play.api.libs.ws.WSClient

@Singleton
final class GitFileService @Inject() (
    private val ws: WSClient,
    implicit val config: GitConfig,
    implicit val ctx: ExecutionContext
) extends GitService {

  /**
   * Downloads the module description from the preview branch of the Git repository.
   */
  def downloadModuleFromPreviewBranch(id: UUID): Future[Option[ModuleProtocol]] =
    for {
      content <- downloadFileContent(GitFilePath(id), config.draftBranch)
      res <- content match {
        case Some(content) => RawModuleParser.parser.parse(content.value)._1.map(Some.apply).toFuture
        case None          => Future.successful(None)
      }
    } yield res

  /**
   * Downloads the content of a file from a Git repository branch.
   */
  def downloadFileContent(path: GitFilePath, branch: Branch): Future[Option[GitFileContent]] =
    download(path, branch).map(_.map(_._1))

  /**
   * Downloads the content of a file from a Git repository branch with its last commit ID.
   */
  def download(path: GitFilePath, branch: Branch): Future[Option[(GitFileContent, Option[CommitId])]] =
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

  /**
   * Checks if a file exists in a specific Git repository branch.
   */
  def fileExists(path: GitFilePath, branch: Branch): Future[Boolean] =
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

  private def fileUrlRaw(path: GitFilePath, branch: Branch) =
    s"${config.baseUrl}/projects/${config.projectId}/repository/files/${urlEncoded(path)}/raw?ref=${branch.value}"
}
