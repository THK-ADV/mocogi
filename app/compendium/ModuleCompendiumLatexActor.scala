package compendium

import akka.actor.{Actor, ActorRef, Props}
import compendium.ModuleCompendiumLatexActor.GenerateLatexFiles
import controllers.FileController
import database.ModuleCompendiumOutput
import database.repo.{AssessmentMethodRepository, LanguageRepository, ModuleCompendiumListRepository, ModuleCompendiumRepository, ModuleTypeRepository, PORepository, PersonRepository, SeasonRepository}
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
  private type FileEntry = (
      String, // ModuleCompendiumList.DB.fullPo
      String, // ModuleCompendiumList.DB.poAbbrev
      Int, // ModuleCompendiumList.DB.poNumber
      String, // ModuleCompendiumList.DB.studyProgram
      Semester, // ModuleCompendiumList.DB.semester
      Path, // Path of Tex File
      Path, // Path of PDF
      PrintingLanguage, // PrintingLanguage
      Option[String] // ModuleCompendiumList.DB.specialization
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
      personRepository: PersonRepository,
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
      personRepository,
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
      private val personRepository: PersonRepository,
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
        poIds = pos.map(_.abbrev)
        mcs <- moduleCompendiumRepository.allFromPos(poIds)
        mts <- moduleTypeRepository.all()
        lang <- languageRepository.all()
        seasons <- seasonRepository.all()
        people <- personRepository.all()
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
                s"${pLang.id}_${semester.id}_${po.fullAbbrev}"
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
                        (
                          po.fullAbbrev,
                          po.abbrev,
                          po.version,
                          po.studyProgram.abbrev,
                          semester,
                          texFile,
                          pdfFile,
                          pLang,
                          po.specialization.map(_.abbrev)
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
        people: Seq[Person],
        ams: Seq[AssessmentMethod],
        po: POShort,
        pLang: PrintingLanguage
    ): StringBuilder =
      printer.print(
        po,
        Some(semester),
        mcs
          .filter(_.metadata.po.mandatory.exists { a =>
            a.po == po.abbrev && a.specialization
              .zip(po.specialization)
              .fold(true)(a => a._1 == a._2.abbrev)
          }),
        mts,
        lang,
        seasons,
        people,
        ams,
        pos
      )(pLang)

    private def moveToGitFolder(
        files: Seq[FileEntry]
    ): Either[String, Seq[FileEntry]] = {
      logger.info(s"moving ${files.size} files to git folder...")
      val destDir = Paths.get(config.glabConfig.mcPath)
      val (failure, success) = files.partitionMap { x =>
        val src = x._6.toAbsolutePath
        val dest = destDir.resolve(src.getFileName)
        try {
          Files.move(
            src,
            dest,
            StandardCopyOption.REPLACE_EXISTING
          )
          Right(x.copy(_6 = dest))
        } catch {
          case NonFatal(_) =>
            logger.error(
              s"failed to move file from ${src.toAbsolutePath} to ${dest.toAbsolutePath} "
            )
            Left(x)
        }
      }

      if (failure.nonEmpty) {
        logger.error("abort! deleting all files...")
        failure.foreach(a => Files.delete(a._6))
        success.foreach(a => Files.delete(a._6))
        val errFiles = failure.map(_._6.toAbsolutePath).mkString(", ")
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

    private def create(xs: Seq[FileEntry]) = {
      val moduleCompendiumFolder =
        Paths.get(config.moduleCompendiumFolderPath).getFileName

      val normalized = xs
        .groupBy(_._1)
        .map { case (_, xs) =>
          def getFilename(lang: PrintingLanguage): String =
            xs
              .find(_._8 == lang)
              .get
              ._7
              .getFileName
              .toString
          val x = xs.head
          val de = getFilename(PrintingLanguage.German)
          val en = getFilename(PrintingLanguage.English)
          ModuleCompendiumList(
            x._1,
            x._2,
            x._3,
            x._9,
            x._4,
            x._5.id,
            FileController.makeURI(moduleCompendiumFolder.toString, de),
            FileController.makeURI(moduleCompendiumFolder.toString, en),
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
