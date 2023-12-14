package service

import database.repo.{ModuleCompendiumRepository, PORepository}
import ops.EitherOps.EStringThrowOps
import ops.FileOps.FileOps0
import play.api.Logging
import play.api.libs.Files.TemporaryFile
import printing.PrintingLanguage
import printing.latex.ModuleCompendiumLatexPrinter
import providers.ConfigReader
import service.LatexCompiler.{compile, exec, getPdf}
import models.Module

import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.Process

@Singleton
final class ModuleCompendiumPreviewService @Inject() (
    printer: ModuleCompendiumLatexPrinter,
    moduleCompendiumRepository: ModuleCompendiumRepository,
    poRepository: PORepository,
    pipeline: MetadataPipeline,
    configReader: ConfigReader,
    implicit val ctx: ExecutionContext
) extends Logging {

  // Left: Preview, Right: Published
  def previewModules(poAbbrev: String): Future[(Seq[Module], Seq[Module])] =
    for {
      modules <- moduleCompendiumRepository.allPreview(
        Map("po_mandatory" -> Seq(poAbbrev))
      )
      modulePreviews <- modifiedInPreview().toFuture
    } yield modules.partition(m => modulePreviews.contains(m.id))

  def previewCompendium(
      poAbbrev: String,
      pLang: PrintingLanguage,
      latexFile: TemporaryFile
  ): Future[Path] =
    for {
      pos <- poRepository.allValidShort()
      po <- pos.find(_.abbrev == poAbbrev) match {
        case Some(value) =>
          Future.successful(value)
        case None =>
          Future.failed(new Throwable(s"po $poAbbrev needs to be valid"))
      }
      _ = switchToStagingBranch()
      // TODO does not consider created modules which are in staging only
      mcIds <- moduleCompendiumRepository.allIdsFromPo(poAbbrev)
      prints = matchPrints(mcIds)
      mcs <- pipeline.parseValidateMany(prints)
      pdf <- mcs match {
        case Left(errs) =>
          Future.failed(new Throwable(errs.map(_.getMessage).mkString("\n")))
        case Right(mcs) =>
          val content = printer.print(
            po,
            mcs
              .map(_._2)
              .filter(_.metadata.validPOs.mandatory.exists { a =>
                a.po.abbrev == po.abbrev && a.specialization
                  .zip(po.specialization)
                  .fold(true)(a => a._1.abbrev == a._2.abbrev)
              }),
            pos
          )(pLang)
          val path = Files.writeString(latexFile, content.toString())
          compile(path).flatMap(_ => getPdf(path)).toFuture
      }
    } yield pdf

  private def matchPrints(moduleIds: Seq[UUID]): Seq[(Option[UUID], Print)] =
    Paths
      .get(configReader.repoPath)
      .resolve(configReader.modulesRootFolder)
      .allFiles(p =>
        moduleIds.exists(id => p.getFileName.toString.startsWith(id.toString))
      )
      .map(p => None -> Print(Files.readString(p)))

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
        .takeWhile(_.startsWith(configReader.modulesRootFolder))
        .map(s =>
          UUID.fromString(
            s.drop(configReader.modulesRootFolder.length + 1).dropRight(3)
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
