package git.api

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.Inject
import git.*
import models.MetadataProtocol
import models.ModuleProtocol
import ops.EitherOps.EThrowableOps
import parsing.RawModuleParser
import printing.html.ModuleHTMLPrinter
import printing.pandoc.PrinterOutput
import printing.pandoc.PrinterOutputType
import service.*

@Singleton
final class GitFileDownloadService @Inject() (
    private val api: GitFileApiService,
    private val commitApiService: GitCommitApiService,
    private val pipeline: MetadataPipeline,
    private val printer: ModuleHTMLPrinter,
    private implicit val config: GitConfig,
    implicit val ctx: ExecutionContext
) {

  def downloadModuleMetadataFromPreviewBranch(path: GitFilePath): Future[Option[(UUID, MetadataProtocol)]] =
    downloadFileContent(path, config.draftBranch).map {
      case Some(content) =>
        val res = RawModuleParser.metadataParser.parse(content.value)._1.toOption
        assert(
          res.isDefined,
          s"module ${path.moduleId} must be successfully parsed from ${config.draftBranch.value} branch"
        )
        res
      case None => None
    }

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

  def downloadModuleFromPreviewBranchAsHTML(module: UUID): Future[Option[String]] =
    for {
      content <- downloadFileContentWithLastModified(GitFilePath(module), config.draftBranch)
      res <- content match {
        case Some((content, lastModified)) =>
          for {
            module <- pipeline.parseValidate(Print(content.value))
            output <- printer
              .print(
                module,
                lastModified.getOrElse(LocalDateTime.now),
                PrinterOutputType.HTMLStandalone
              )
            res <- output match {
              case Left(err)                          => Future.failed(err)
              case Right(PrinterOutput.Text(c, _, _)) => Future.successful(c)
              case Right(PrinterOutput.File(_, _)) =>
                Future.failed(new Exception("expected standalone HTML, but was a file"))
            }
          } yield Some(res)
        case None =>
          Future.successful(None)
      }
    } yield res
}
