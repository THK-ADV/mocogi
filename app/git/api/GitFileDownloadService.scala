package git.api

import java.util.UUID
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.Inject
import git.Branch
import git.CommitId
import git.GitConfig
import git.GitFileContent
import git.GitFilePath
import models.MetadataProtocol
import models.ModuleProtocol
import ops.EitherOps.EThrowableOps
import parsing.RawModuleParser
import printing.html.ModuleHTMLPrinter
import printing.pandoc.PrinterOutput
import printing.pandoc.PrinterOutputType
import printing.PrintingLanguage
import service.*

@Singleton
final class GitFileDownloadService @Inject() (
    private val api: GitFileApiService,
    private val pipeline: MetadataPipeline,
    private val printer: ModuleHTMLPrinter,
    private implicit val config: GitConfig,
    implicit val ctx: ExecutionContext
) {

  def downloadModuleMetadataFromPreviewBranch(path: GitFilePath): Future[Option[(UUID, MetadataProtocol)]] =
    downloadFileContent(path, config.draftBranch).map {
      case Some((content, _)) =>
        val res = RawModuleParser.metadataParser.parse(content.value)._1.toOption
        assert(
          res.isDefined,
          s"module ${path.moduleId} must be successfully parsed from ${config.draftBranch.value} branch"
        )
        res
      case None => None
    }

  def downloadModuleFromPreviewBranch(id: UUID): Future[Option[(ModuleProtocol, Option[CommitId])]] =
    for {
      content <- downloadFileContent(GitFilePath(id), config.draftBranch)
      res <- content match {
        case Some((content, commitId)) =>
          RawModuleParser.parser
            .parse(content.value)
            ._1
            .map(a => Some(a, commitId))
            .toFuture
        case None =>
          Future.successful(None)
      }
    } yield res

  def downloadFileContent(path: GitFilePath, branch: Branch): Future[Option[(GitFileContent, Option[CommitId])]] =
    api.download(path, branch)

  def downloadModuleFromPreviewBranchAsHTML(
      module: UUID
  )(implicit lang: PrintingLanguage): Future[Option[String]] =
    for {
      content <- downloadFileContent(GitFilePath(module), config.draftBranch)
      res <- content match {
        case Some((content, _)) =>
          for {
            module <- pipeline.parseValidate(Print(content.value))
            output <- printer
              .print(
                module,
                lang,
                None,
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
