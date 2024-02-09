package catalog

import akka.actor.{Actor, Props}
import controllers.FileController
import database.repo.core._
import database.repo.{
  ModuleCatalogGenerationRequestRepository,
  ModuleCatalogRepository,
  ModuleRepository
}
import database.table.ModuleCatalogEntry
import database.view.StudyProgramViewRepository
import git.api.{GitAvailabilityChecker, GitMergeRequestApiService}
import models._
import models.core._
import ops.EitherOps.EStringThrowOps
import ops.FileOps.FileOps0
import ops.LoggerOps
import play.api.Logging
import printing.PrintingLanguage
import printing.latex.ModuleCatalogLatexPrinter
import service.LatexCompiler

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.LocalDateTime
import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable
import scala.concurrent.ExecutionContext
import scala.sys.process._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object ModuleCatalogLatexActor {
  private case class ModuleCatalogFile(
      studyProgram: StudyProgramView,
      semester: Semester,
      texFile: Path,
      pdfFile: Path,
      lang: PrintingLanguage
  )

  case class GenerateLatexFiles(request: ModuleCatalogGenerationRequest)
      extends AnyVal

  case class Config(
      tmpFolderPath: String,
      moduleCatalogFolderPath: String,
      repoPath: String,
      mcPath: String,
      pushScriptPath: String,
      mainBranch: Branch,
      moduleCatalogLabel: String
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
      moduleCatalogGenerationRepo: ModuleCatalogGenerationRequestRepository,
      gitMergeRequestApiService: GitMergeRequestApiService,
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
      moduleCatalogGenerationRepo,
      gitMergeRequestApiService,
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
      private val moduleCatalogGenerationRepo: ModuleCatalogGenerationRequestRepository,
      private val gitMergeRequestApiService: GitMergeRequestApiService,
      private val config: Config,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

    import LatexCompiler._

    private def tmpFolder = Paths.get(config.tmpFolderPath)

    private def generate(request: ModuleCatalogGenerationRequest) = {
      val semester = Semester(request.semesterId)
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
        branch = Branch(semester.id)
        commitMessage =
          s"Module Catalog for ${semester.enLabel} ${semester.year}"
        commitSuccess <- commit(branch, commitMessage).toFuture
        res <-
          if (commitSuccess) {
            for {
              (mrId, mrStatus) <- createMergeRequest(branch, commitMessage)
              _ = logger.info(
                s"successfully created merge request with id ${mrId.value}"
              )
              created <- createCatalogFiles(files)
              _ = logger.info(
                s"successfully created ${created.size} module catalogs"
              )
              _ <- moduleCatalogGenerationRepo.update(mrId, mrStatus, request)
            } yield logger.info(
              s"successfully updated generation request to new id: ${mrId.value} and status ${mrStatus.id}"
            )
          } else {
            logger.info("nothing to commit!")
            moduleCatalogGenerationRepo
              .delete(request.mergeRequestId)
              .map(_ =>
                logger.info(
                  s"successfully deleted generation request with id ${request.mergeRequestId.value}"
                )
              )
          }
      } yield res
    }

    private def createMergeRequest(branch: Branch, commitMessage: String) =
      gitMergeRequestApiService.create(
        branch,
        config.mainBranch,
        commitMessage,
        "",
        needsApproval = false,
        config.moduleCatalogLabel
      )

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
      val destDir = Paths.get(config.mcPath)
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

    private def commit(
        branch: Branch,
        message: String
    ): Either[String, Boolean] = {
      val process = Process(
        command = Seq(
          "/bin/bash",
          config.pushScriptPath,
          config.mainBranch.value,
          branch.value,
          message
        ),
        cwd = Paths.get(config.repoPath).toAbsolutePath.toFile
      )
      val res = execRes(process) {
        case 1 => s"failed git switch ${config.mainBranch.value}"
        case 2 => "failed git pull"
        case 3 => s"failed git swtich -c ${branch.value}"
        case 4 => "failed git add"
        case 5 => "failed git commit"
        case 6 => s"failed git push origin ${branch.value}"
        case 7 => "no changes"
      }
      res match {
        case Right(_) =>
          Right(true)
        case Left((stdErr, codeErr)) =>
          if (codeErr.exists(_._1 == 7)) {
            Right(false)
          } else {
            Left(codeErr.fold(stdErr)(_._2))
          }
      }
    }

    private def createCatalogFiles(xs: Iterable[ModuleCatalogFile]) = {
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

    override def receive: Receive = { case GenerateLatexFiles(request) =>
      logger.info("start generating module catalogs")
      generate(request) onComplete {
        case Success(_) => logger.info("finished!")
        case Failure(e) => logFailure(e)
      }
    }
  }
}
