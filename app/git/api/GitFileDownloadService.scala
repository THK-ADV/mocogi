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
import service.*

@deprecated("replace with Git CLI call")
@Singleton
final class GitFileDownloadService @Inject() (
    private val api: GitFileApiService,
    private val commitApiService: GitCommitApiService,
    private val pipeline: MetadataPipeline,
    private implicit val config: GitConfig,
    implicit val ctx: ExecutionContext
) {

  def downloadModuleFromPreviewBranch(id: UUID): Future[Option[ModuleProtocol]] =
    for {
      content <- downloadFileContent(GitFilePath(id), config.draftBranch)
      res <- content match {
        case Some(content) =>
          RawModuleParser.parser
            .parse(content.value)
            ._1
            .map(Some.apply)
            .toFuture
        case None =>
          Future.successful(None)
      }
    } yield res

  def downloadModuleFromPreviewBranchWithLastModified(
      id: UUID
  ): Future[Option[(ModuleProtocol, Option[LocalDateTime])]] =
    for {
      content <- downloadFileContentWithLastModified(GitFilePath(id), config.draftBranch)
      res <- content match {
        case Some((content, lastModified)) =>
          RawModuleParser.parser
            .parse(content.value)
            ._1
            .map(a => Some(a, lastModified))
            .toFuture
        case None =>
          Future.successful(None)
      }
    } yield res

  def downloadFileContent(path: GitFilePath, branch: Branch): Future[Option[GitFileContent]] =
    api.download(path, branch).map(_.map(_._1))

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
