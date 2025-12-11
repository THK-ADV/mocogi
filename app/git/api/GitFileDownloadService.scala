package git.api

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.Inject
import git.*
import models.ModuleProtocol
import ops.EitherOps.EThrowableOps
import parsing.RawModuleParser

@Singleton
final class GitFileDownloadService @Inject() (
    private val api: GitFileApiService,
    private val commitApiService: GitCommitApiService,
    implicit val ctx: ExecutionContext
) {

  private given gitConfig: GitConfig = api.config

  /**
   * Downloads the module description from the preview branch of the Git repository
   *
   * @param id The UUID of the module to download.
   * @return Some(ModuleProtocol) if the module is successfully parsed,
   *         or None if the file is not found or parsing fails
   */
  def downloadModuleFromPreviewBranch(id: UUID): Future[Option[ModuleProtocol]] = {

    for {
      content <- downloadFileContent(GitFilePath(id), gitConfig.draftBranch)
      res <- content match {
        case Some(content) => RawModuleParser.parser.parse(content.value)._1.map(Some.apply).toFuture
        case None          => Future.successful(None)
      }
    } yield res
  }

  /**
   * Downloads the content of a file from the specified Git repository branch.
   *
   * @param path   The path of the file in the Git repository.
   * @param branch The branch from which the file content should be downloaded.
   * @return Some(GitFileContent) if the file is found, or None if the file is not present.
   */
  def downloadFileContent(path: GitFilePath, branch: Branch): Future[Option[GitFileContent]] =
    api.download(path, branch).map(_.map(_._1))

  /**
   * Downloads the content of a file from a Git repository branch along with its last modification date.
   *
   * @param path   The path of the file in the Git repository.
   * @param branch The branch from which the file content and last modification date should be downloaded.
   * @return Option, which is:
   *         - Some((GitFileContent, Some(LocalDateTime))) if the file is found and the last modification date is available.
   *         - Some((GitFileContent, None)) if the file is found, but the last modification date is not available.
   *         - None if the file is not present in the specified branch.
   */
  def downloadFileContentWithLastModified(
      path: GitFilePath,
      branch: Branch
  ): Future[Option[(GitFileContent, Option[LocalDateTime])]] =
    api.download(path, branch).flatMap {
      case Some((content, Some(lastCommit))) =>
        commitApiService.getCommitDate(lastCommit.value).map(d => Some(content, Some(d)))
      case Some((content, None)) =>
        Future.successful(Some(content, None))
      case None =>
        Future.successful(None)
    }
}
