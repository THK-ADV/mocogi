package compendium

import akka.actor.{Actor, ActorRef, Props}
import compendium.ModuleCompendiumLatexActor.GenerateLatexFiles
import controllers.FileController
import database.ModuleCompendiumOutput
import database.repo.{
  AssessmentMethodRepository,
  IdentityRepository,
  LanguageRepository,
  ModuleCompendiumListRepository,
  ModuleCompendiumRepository,
  ModuleTypeRepository,
  PORepository,
  SeasonRepository
}
import git.api.GitAvailabilityChecker
import models.core._
import models.{ModuleCompendiumList, POShort, Semester}
import ops.EitherOps.{EOps, EStringThrowOps}
import ops.FileOps.FileOps0
import ops.LoggerOps
import play.api.Logging
import printing.PrintingLanguage
import printing.latex.ModuleCompendiumLatexPrinter
import service.LatexCompiler

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.LocalDateTime
import javax.inject.Singleton
import scala.collection.parallel.CollectionConverters._
import scala.concurrent.ExecutionContext
import scala.sys.process._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton
final class ModuleCompendiumLatexActor(actor: ActorRef) {
  def generateLatexFiles(semester: Semester): Unit =
    actor ! GenerateLatexFiles(semester)
}

object ModuleCompendiumLatexActor {
  private case class ModuleCatalogFile(
      fullPoId: String,
      poId: String,
      poNumber: Int,
      studyProgramId: String,
      semester: Semester,
      texFile: Path,
      pdfFile: Path,
      lang: PrintingLanguage,
      specializationId: Option[String]
  )

  private case class GenerateLatexFiles(semester: Semester) extends AnyVal

  case class Config(
      tmpFolderPath: String,
      moduleCompendiumFolderPath: String,
      glabConfig: GlabConfig
  )

  case class GlabConfig(
      repoPath: String,
      mcPath: String,
      pushScriptPath: String,
      mainBranch: String
  )

  def props(
      gitAvailabilityChecker: GitAvailabilityChecker,
      printer: ModuleCompendiumLatexPrinter,
      moduleCompendiumRepository: ModuleCompendiumRepository,
      moduleCompendiumListRepository: ModuleCompendiumListRepository,
      poRepository: PORepository,
      moduleTypeRepository: ModuleTypeRepository,
      languageRepository: LanguageRepository,
      seasonRepository: SeasonRepository,
      identityRepository: IdentityRepository,
      assessmentMethodRepository: AssessmentMethodRepository,
      config: Config,
      ctx: ExecutionContext
  ) = Props(
    new Impl(
      gitAvailabilityChecker,
      printer,
      moduleCompendiumRepository,
      moduleCompendiumListRepository,
      poRepository,
      moduleTypeRepository,
      languageRepository,
      seasonRepository,
      identityRepository,
      assessmentMethodRepository,
      config,
      ctx
    )
  )

  private final class Impl(
      private val apiAvailableService: GitAvailabilityChecker,
      private val printer: ModuleCompendiumLatexPrinter,
      private val moduleCompendiumRepository: ModuleCompendiumRepository,
      private val moduleCompendiumListRepository: ModuleCompendiumListRepository,
      private val poRepository: PORepository,
      private val moduleTypeRepository: ModuleTypeRepository,
      private val languageRepository: LanguageRepository,
      private val seasonRepository: SeasonRepository,
      private val identityRepository: IdentityRepository,
      private val assessmentMethodRepository: AssessmentMethodRepository,
      private val config: Config,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

    import LatexCompiler._

    private def tmpFolder = Paths.get(config.tmpFolderPath)

    private def generate(semester: Semester) =
      for {
        _ <- apiAvailableService.checkAvailability()
        pos <- poRepository.allValidShort()
        poIds = pos.map(_.id)
        mcs <- moduleCompendiumRepository.allFromPos(poIds)
        mts <- moduleTypeRepository.all()
        lang <- languageRepository.all()
        seasons <- seasonRepository.all()
        people <- identityRepository.all()
        ams <- assessmentMethodRepository.all()
        files <- {
          val res = pos.par
            .flatMap(po => PrintingLanguage.all().map(po -> _))
            .map { case (po, pLang) =>
              val content = print(
                semester,
                pos,
                mcs,
                mts,
                lang,
                seasons,
                people,
                ams,
                po,
                pLang
              )
              val filename =
                s"${pLang.id}_${semester.id}_${po.fullId}"
              createTexFile(filename, content) match {
                case Left(err) =>
                  Left(None, err)
                case Right(texFile) =>
                  (for {
                    _ <- compile(texFile)
                    _ = clear(texFile)
                    pdf <- getPdf(texFile)
                    res <- movePdf(pdf, filename)
                  } yield res) match {
                    case Left(err) =>
                      markFileAsBroken(texFile) match {
                        case Left(err) =>
                          Left(Some(texFile), err)
                        case Right(texFile) =>
                          Left(Some(texFile), err)
                      }
                    case Right(pdfFile) =>
                      Right(
                        ModuleCatalogFile(
                          po.fullId,
                          po.id,
                          po.version,
                          po.studyProgram.id,
                          semester,
                          texFile,
                          pdfFile,
                          pLang,
                          po.specialization.map(_.id)
                        )
                      )
                  }
              }
            }
            .seq
          res.collect { case Left((file, msg)) => logger.error(s"$file\n$msg") }
          val files = res.collect { case Right(r) => r }
          moveToGitFolder(files).toFuture
        }
        _ <- commit(semester).toFuture
        res <- create(files)
      } yield res

    private def print(
        semester: Semester,
        pos: Seq[POShort],
        mcs: Seq[ModuleCompendiumOutput],
        mts: Seq[ModuleType],
        lang: Seq[Language],
        seasons: Seq[Season],
        people: Seq[Identity],
        ams: Seq[AssessmentMethod],
        po: POShort,
        pLang: PrintingLanguage
    ): StringBuilder =
      printer.print(
        po,
        Some(semester),
        mcs
          .filter(_.metadata.po.mandatory.exists { a =>
            a.po == po.id && a.specialization
              .zip(po.specialization)
              .fold(true)(a => a._1 == a._2.id)
          }),
        mts,
        lang,
        seasons,
        people,
        ams,
        pos
      )(pLang)

    private def moveToGitFolder(
        files: Seq[ModuleCatalogFile]
    ): Either[String, Seq[ModuleCatalogFile]] = {
      logger.info(s"moving ${files.size} files to git folder...")
      val destDir = Paths.get(config.glabConfig.mcPath)
      val (failure, success) = files.partitionMap { f =>
        val src = f.texFile.toAbsolutePath
        val dest = destDir.resolve(src.getFileName)
        try {
          Files.move(
            src,
            dest,
            StandardCopyOption.REPLACE_EXISTING
          )
          Right(f.copy(texFile = dest))
        } catch {
          case NonFatal(_) =>
            logger.error(
              s"failed to move file from ${src.toAbsolutePath} to ${dest.toAbsolutePath} "
            )
            Left(f)
        }
      }

      if (failure.nonEmpty) {
        logger.error("abort! deleting all files...")
        failure.foreach(a => Files.delete(a.texFile))
        success.foreach(a => Files.delete(a.texFile))
        val errFiles = failure.map(_.texFile.toAbsolutePath).mkString(", ")
        Left(s"failed moving files: $errFiles")
      } else {
        logger.info("finished moving files!")
        Right(success)
      }
    }

    private def commit(semester: Semester): Either[String, String] = {
      val branchName = semester.id
      val process = Process(
        command = Seq(
          "/bin/bash",
          config.glabConfig.pushScriptPath,
          config.glabConfig.mainBranch,
          branchName,
          s"Module Compendium Entries for ${semester.enLabel} ${semester.year}",
          s"Module Handbook for ${semester.enLabel} ${semester.year}"
        ),
        cwd = Paths.get(config.glabConfig.repoPath).toAbsolutePath.toFile
      )
      val res = execRes(process) {
        case 1 => "failed to commit"
        case 2 => "failed to create the merge request"
        case 3 => "failed to merge the merge request"
      }
      res.fold(a => logger.error(a._1), logger.info(_))
      res.mapErr(a => a._2.getOrElse(a._1))
    }

    private def create(xs: Seq[ModuleCatalogFile]) = {
      def getPdfFileName(
          xs: Seq[ModuleCatalogFile],
          lang: PrintingLanguage
      ): String =
        xs
          .find(_.lang == lang)
          .get
          .pdfFile
          .getFileName
          .toString

      val moduleCompendiumFolder =
        Paths.get(config.moduleCompendiumFolderPath).getFileName

      val normalized = xs
        .groupBy(_.fullPoId)
        .map { case (_, xs) =>
          val file = xs.head
          val dePdf = getPdfFileName(xs, PrintingLanguage.German)
          val enPdf = getPdfFileName(xs, PrintingLanguage.English)
          ModuleCompendiumList(
            file.fullPoId,
            file.poId,
            file.poNumber,
            file.specializationId,
            file.studyProgramId,
            file.semester.id,
            FileController.makeURI(moduleCompendiumFolder.toString, dePdf),
            FileController.makeURI(moduleCompendiumFolder.toString, enPdf),
            LocalDateTime.now()
          )
        }
        .toSeq
      moduleCompendiumListRepository.createOrUpdateMany(normalized)
    }

    private def createTexFile(
        name: String,
        content: StringBuilder
    ): Either[String, Path] = tmpFolder.createFile(s"$name.tex", content)

    private def movePdf(file: Path, newFilename: String): Either[String, Path] =
      try {
        Right(
          Files.move(
            file,
            Paths.get(config.moduleCompendiumFolderPath, s"$newFilename.pdf"),
            StandardCopyOption.REPLACE_EXISTING
          )
        )
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }

    override def receive: Receive = { case GenerateLatexFiles(semester) =>
      logger.info("start generating module compendium list")
      generate(semester) onComplete {
        case Success(value) =>
          logSuccess(
            s"created ${value.size} module compendium list normalized"
          )
        case Failure(e) => logFailure(e)
      }
    }
  }
}
