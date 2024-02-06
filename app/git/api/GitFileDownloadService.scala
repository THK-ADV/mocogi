package git.api

import com.google.inject.Inject
import database._
import git.{GitConfig, GitFileContent, GitFilePath}
import models.Branch
import printing.PrintingLanguage
import printing.html.ModuleHTMLPrinter
import printing.pandoc.{PrinterOutput, PrinterOutputType}
import service._

import java.util.UUID
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitFileDownloadService @Inject() (
    private val api: GitFileDownloadApiService,
    private val pipeline: MetadataPipeline,
    private val printer: ModuleHTMLPrinter,
    private implicit val config: GitConfig,
    implicit val ctx: ExecutionContext
) {

  def downloadModuleFromPreviewBranch(
      id: UUID
  ): Future[Option[ModuleOutput]] = {
    val path = GitFilePath(id)
    for {
      content <- downloadFileContent(path, config.draftBranch)
      res <- content match {
        case Some(content) =>
          pipeline.parse(Print(content.value), path).map(Some.apply)
        case None =>
          Future.successful(None)
      }
    } yield res
  }

  def downloadFileContent(
      path: GitFilePath,
      branch: Branch
  ): Future[Option[GitFileContent]] =
    api.download(path, branch)

  def downloadModuleFromPreviewBranchAsHTML(
      module: UUID
  )(implicit lang: PrintingLanguage): Future[Option[String]] = {
    val path = GitFilePath(module)
    for {
      content <- downloadFileContent(path, config.draftBranch)
      res <- content match {
        case Some(content) =>
          for {
            mc <- pipeline.parseValidate(Print(content.value))
            output <- printer
              .print(
                mc,
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
}
