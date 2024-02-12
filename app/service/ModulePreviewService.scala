package service

import database.view.StudyProgramViewRepository
import models.ModuleCore
import ops.EitherOps.EStringThrowOps
import ops.LoggerOps
import play.api.Logging
import play.api.libs.Files.TemporaryFile
import printing.PrintingLanguage
import printing.latex.ModuleCatalogLatexPrinter
import providers.ConfigReader
import service.LatexCompiler.{compile, exec, getPdf}
import service.ModulePreviewService.containsPO

import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.sys.process.Process

@Singleton
final class ModulePreviewService @Inject() (
    printer: ModuleCatalogLatexPrinter,
    moduleService: ModuleService,
    studyProgramViewRepo: StudyProgramViewRepository,
    pipeline: MetadataPipeline,
    configReader: ConfigReader,
    implicit val ctx: ExecutionContext
) extends Logging
    with LoggerOps {

  // Left: Preview, Right: Published
  def previewModules(poId: String): Future[(Seq[ModuleCore], Seq[ModuleCore])] =
    for {
      modules <- moduleService.allModuleCore(
        Map("po_mandatory" -> Seq(poId))
      )
      modulePreviews <- modifiedInPreview().toFuture
    } yield modules.partition(m => modulePreviews.contains(m.id))

  def previewCatalog(
      poId: String,
      pLang: PrintingLanguage,
      latexFile: TemporaryFile
  ): Future[Path] = {
    for {
      studyPrograms <- studyProgramViewRepo.all()
      studyProgram <- studyPrograms.find(_.poId == poId) match {
        case Some(value) =>
          Future.successful(value)
        case None =>
          Future.failed(
            new Throwable(s"study program's po $poId needs to be valid")
          )
      }
      _ <- switchToStagingBranch().toFuture
      modules = getAllModulesFromPreview(studyProgram.poId)
      modules <- pipeline.parseValidateMany(modules)
      pdf <- modules match {
        case Left(errs) =>
          Future.failed(new Throwable(errs.map(_.getMessage).mkString("\n")))
        case Right(modules) =>
          val content = printer.print(
            studyProgram,
            modules
              .map(_._2)
              .filter(_.metadata.pos.mandatory.exists { a =>
                a.po.id == studyProgram.poId && a.specialization
                  .zip(studyProgram.specialization)
                  .fold(true)(a => a._1.id == a._2.id)
              }),
            studyPrograms
          )(pLang)
          val path = Files.writeString(latexFile, content.toString())
          compile(path).flatMap(_ => getPdf(path)).toFuture
      }
    } yield pdf
  }

  private def getAllModulesFromPreview(
      poId: String
  ): Seq[(Option[UUID], Print)] = {
    val folder =
      Paths.get(configReader.repoPath).resolve(configReader.gitModulesFolder)
    Files
      .walk(folder)
      .iterator()
      .asScala
      .drop(1) // drop root directory
      .map(Files.readString)
      .filter(containsPO(_, poId))
      .map(None -> Print(_))
      .toSeq
  }

  private def switchToStagingBranch(): Either[String, String] =
    exec(
      Process(
        command = Seq(
          "/bin/bash",
          configReader.switchBranchScriptPath,
          configReader.draftBranch
        ),
        cwd = Paths.get(configReader.repoPath).toAbsolutePath.toFile
      )
    )

  private def modifiedInPreview(): Either[String, List[UUID]] = {
    // TODO resolve title and abbrev in case of change
    def parse(s: String): List[UUID] =
      s.linesIterator
        .takeWhile(s => s.headOption.contains('M'))
        .map(_.drop(1).dropWhile(_.isWhitespace))
        .takeWhile(_.startsWith(configReader.gitModulesFolder))
        .map(s =>
          UUID.fromString(
            s.drop(configReader.gitModulesFolder.length + 1).dropRight(3)
          )
        )
        .toList

    exec(
      Process(
        command = Seq(
          "/bin/bash",
          configReader.diffPreviewScriptPath,
          configReader.mainBranch,
          configReader.draftBranch
        ),
        cwd = Paths.get(configReader.repoPath).toAbsolutePath.toFile
      )
    ).map(parse)
  }
}

object ModulePreviewService {
  def containsPO(input: String, poId: String): Boolean = {
    val start = input.indexOf("po_mandatory:\n")
    if (start < 0) return false
    val end = input.lastIndexOf("---")
    if (end < 0) return false
    val input0 = input.slice(start, end)
    input0.contains(s"- study_program: study_program.$poId")
  }
}
