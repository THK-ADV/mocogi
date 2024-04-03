package git.api

import com.google.inject.Inject
import git.{Branch, GitConfig, GitFileContent, GitFilePath}
import models.ModuleProtocol
import ops.EitherOps.EThrowableOps
import parsing.RawModuleParser
import printing.PrintingLanguage
import printing.html.ModuleHTMLPrinter
import printing.pandoc.{PrinterOutput, PrinterOutputType}
import service._

import java.util.UUID
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitFileDownloadService @Inject() (
    private val api: GitFileApiService,
    private val pipeline: MetadataPipeline,
    private val printer: ModuleHTMLPrinter,
    private implicit val config: GitConfig,
    implicit val ctx: ExecutionContext
) {

  def downloadModuleFromPreviewBranch(
      id: UUID
  ): Future[Option[ModuleProtocol]] =
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

  def downloadFileContent(
      path: GitFilePath,
      branch: Branch
  ): Future[Option[GitFileContent]] =
    api.download(path, branch)

  def downloadModuleFromPreviewBranchAsHTML(
      module: UUID
  )(implicit lang: PrintingLanguage): Future[Option[String]] =
    for {
      content <- downloadFileContent(GitFilePath(module), config.draftBranch)
      res <- content match {
        case Some(content) =>
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
                Future.failed(
                  new Throwable("expected standalone HTML, but was a file")
                )
            }
          } yield Some(res)
        case None =>
          Future.successful(None)
      }
    } yield res
}
