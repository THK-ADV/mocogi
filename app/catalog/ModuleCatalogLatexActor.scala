package catalog

import akka.actor.{Actor, ActorRef, Props}
import catalog.ModuleCatalogLatexActor.GenerateLatexFiles
import controllers.FileController
import database.repo.core._
import database.repo.{ModuleCatalogRepository, ModuleRepository}
import database.table.ModuleCatalogEntry
import database.view.StudyProgramViewRepository
import git.api.GitAvailabilityChecker
import models.core._
import models.{ModuleProtocol, Semester, StudyProgramView}
import ops.EitherOps.{EOps, EStringThrowOps}
import ops.FileOps.FileOps0
import ops.LoggerOps
import play.api.Logging
import printing.PrintingLanguage
import printing.latex.ModuleCatalogLatexPrinter
import service.LatexCompiler

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.LocalDateTime
import javax.inject.Singleton
import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable
import scala.concurrent.ExecutionContext
import scala.sys.process._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton
final class ModuleCatalogLatexActor(actor: ActorRef) {
  def generateLatexFiles(semester: Semester): Unit =
    actor ! GenerateLatexFiles(semester)
}

object ModuleCatalogLatexActor {
  private case class ModuleCatalogFile(
      studyProgram: StudyProgramView,
      semester: Semester,
      texFile: Path,
      pdfFile: Path,
      lang: PrintingLanguage
  )

  private case class GenerateLatexFiles(semester: Semester) extends AnyVal

  case class Config(
      tmpFolderPath: String,
      moduleCatalogFolderPath: String,
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
      printer: ModuleCatalogLatexPrinter,
      moduleRepository: ModuleRepository,
      catalogRepository: ModuleCatalogRepository,
      studyProgramViewRepo: StudyProgramViewRepository,
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
      moduleRepository,
      catalogRepository,
      studyProgramViewRepo,
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
      private val printer: ModuleCatalogLatexPrinter,
      private val moduleRepository: ModuleRepository,
      private val catalogRepository: ModuleCatalogRepository,
      private val studyProgramViewRepo: StudyProgramViewRepository,
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
        sps <- studyProgramViewRepo.all()
        poIds = sps.map(_.poId)
        ms <- moduleRepository.allFromPos(poIds)
        mts <- moduleTypeRepository.all()
        lang <- languageRepository.all()
        seasons <- seasonRepository.all()
        people <- identityRepository.all()
        ams <- assessmentMethodRepository.all()
        files <- {
          val res = sps.par
            .flatMap(po => PrintingLanguage.all().map(po -> _))
            .map { case (sp, pLang) =>
              val content = print(
                semester,
                sps,
                ms,
                mts,
                lang,
                seasons,
                people,
                ams,
                sp,
                pLang
              )
              val filename =
                s"${pLang.id}_${semester.id}_${sp.fullPoId}"
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
                          sp,
                          semester,
                          texFile,
                          pdfFile,
                          pLang
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
        //        _ <- commit(semester).toFuture
        res <- create(files)
      } yield res

    private def print(
        semester: Semester,
        sps: Seq[StudyProgramView],
        ms: Seq[ModuleProtocol],
        mts: Seq[ModuleType],
        lang: Seq[ModuleLanguage],
        seasons: Seq[Season],
        people: Seq[Identity],
        ams: Seq[AssessmentMethod],
        sp: StudyProgramView,
        pLang: PrintingLanguage
    ): StringBuilder =
      printer.print(
        sp,
        Some(semester),
        ms
          .filter(_.metadata.po.mandatory.exists { a =>
            a.po == sp.poId && a.specialization
              .zip(sp.specialization)
              .fold(true)(a => a._1 == a._2.id)
          }),
        mts,
        lang,
        seasons,
        people,
        ams,
        sps
      )(pLang)

    private def moveToGitFolder(
        files: Iterable[ModuleCatalogFile]
    ): Either[String, Iterable[ModuleCatalogFile]] = {
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
          s"Module Catalog for ${semester.enLabel} ${semester.year}",
          s"Module Catalog for ${semester.enLabel} ${semester.year}"
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

    private def create(xs: Iterable[ModuleCatalogFile]) = {
      def getPdfFileName(
          xs: Iterable[ModuleCatalogFile],
          lang: PrintingLanguage
      ): String =
        xs
          .find(_.lang == lang)
          .get
          .pdfFile
          .getFileName
          .toString

      val moduleCatalogFolder =
        Paths.get(config.moduleCatalogFolderPath).getFileName

      val normalized = xs
        .groupBy(_.studyProgram.fullPoId)
        .map { case (_, xs) =>
          val file = xs.head
          val dePdf = getPdfFileName(xs, PrintingLanguage.German)
          val enPdf = getPdfFileName(xs, PrintingLanguage.English)
          ModuleCatalogEntry(
            file.studyProgram.fullPoId.id,
            file.studyProgram.poId,
            file.studyProgram.specialization.map(_.id),
            file.studyProgram.studyProgram.id,
            file.semester.id,
            FileController.makeURI(moduleCatalogFolder.toString, dePdf),
            FileController.makeURI(moduleCatalogFolder.toString, enPdf),
            LocalDateTime.now()
          )
        }
        .toSeq
      catalogRepository.createOrUpdateMany(normalized)
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
            Paths.get(config.moduleCatalogFolderPath, s"$newFilename.pdf"),
            StandardCopyOption.REPLACE_EXISTING
          )
        )
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }

    override def receive: Receive = { case GenerateLatexFiles(semester) =>
      logger.info("start generating module catalogs")
      generate(semester) onComplete {
        case Success(value) =>
          logSuccess(s"created ${value.size} module catalogs")
        case Failure(e) => logFailure(e)
      }
    }
  }
}
