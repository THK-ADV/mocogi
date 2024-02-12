package catalog

import controllers.FileController
import database.repo.core._
import database.repo.{
  ModuleCatalogGenerationRequestRepository,
  ModuleCatalogRepository,
  ModuleRepository
}
import database.table.ModuleCatalogEntry
import database.view.StudyProgramViewRepository
import git.GitFilePath
import git.api.{
  GitAvailabilityChecker,
  GitBranchApiService,
  GitMergeRequestApiService
}
import models._
import ops.FileOps.FileOps0
import play.api.Logging
import printing.PrintingLanguage
import printing.latex.ModuleCatalogLatexPrinter
import service.LatexCompiler

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
final class ModuleCatalogService @Inject() (
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
    private val electivesCatalogService: ElectivesCatalogService,
    private val config: ModuleCatalogConfig,
    private val commitService: ModuleCatalogCommitService,
    private val branchApiService: GitBranchApiService,
    implicit val ctx: ExecutionContext
) extends Logging {

  import LatexCompiler._

  def createAndOpenMergeRequest(
      r: ModuleCatalogGenerationRequest
  ): Future[Unit] = {
    implicit val semester: Semester = Semester(r.semesterId)
    implicit val branch: Branch = Branch(semester.id)

    for {
      _ <- apiAvailableService.checkAvailability()
      _ <- branchApiService.createBranch(branch, config.mainBranch)
      (catalogFiles, brokenFiles, electivesFile) <- createCatalogs
        .recoverWith { case NonFatal(e) =>
          logger.error("failed creating catalogs! recovering...")
          deleteBranch(branch).flatMap(_ => Future.failed(e))
        }
      _ <- commit(catalogFiles, brokenFiles, electivesFile)
        .recoverWith { case NonFatal(e) =>
          logger.error("commit failed! recovering...")
          deleteAllFiles(electivesFile._1 :: brokenFiles, catalogFiles)
          deleteBranch(branch).flatMap(_ => Future.failed(e))
        }
      (mrId, mrStatus) <- createMergeRequest
        .recoverWith { case NonFatal(e) =>
          logger.error("failed to create merge request! recovering...")
          deleteAllFiles(electivesFile._1 :: brokenFiles, catalogFiles)
          deleteBranch(branch).flatMap(_ => Future.failed(e))
        }
      _ <- createCatalogFiles(catalogFiles)
        .recoverWith { case NonFatal(e) =>
          logger.error("failed to create catalog files! recovering...")
          deleteAllFiles(electivesFile._1 :: brokenFiles, catalogFiles)
          deleteMergeRequest(mrId)
            .flatMap(_ => deleteBranch(branch))
            .flatMap(_ => Future.failed(e))
        }
      _ <- moduleCatalogGenerationRepo.update(mrId, mrStatus, r)
    } yield tmpFolder.deleteContentsOfDirectory()
  }

  private def tmpFolder = Paths.get(config.tmpFolderPath)

  private def createCatalogs(implicit semester: Semester) =
    for {
      (catalogFiles, brokenFiles) <- printAndCompile(semester)
      catalogPdfFiles <- movePDFsToOutputFolder(catalogFiles)
        .recoverWith { case NonFatal(e) =>
          logger.error("failed moving pdf files! recovering...")
          deleteAllFiles(brokenFiles, Nil)
          Future.failed(e)
        }
      electivesFile <- electivesCatalogService
        .create(semester)
        .recoverWith { case NonFatal(e) =>
          logger.error("failed creating electives! recovering...")
          deleteAllFiles(brokenFiles, catalogPdfFiles)
          Future.failed(e)
        }
    } yield (catalogPdfFiles, brokenFiles, electivesFile)

  private def commit(
      catalogFiles: List[ModuleCatalogFile[Path]],
      brokenFiles: List[Path],
      electivesFile: (Path, String)
  )(implicit semester: Semester, branch: Branch) = {
    def toGitPath(path: Path) = GitFilePath(
      s"${config.moduleCatalogGitPath}/${path.getFileName.toString}"
    )
    val files = ListBuffer.empty[(GitFilePath, String)]
    catalogFiles.foreach(f => files += ((toGitPath(f.texFile), f.content)))
    brokenFiles.foreach(f => files += ((toGitPath(f), Files.readString(f))))
    files += ((toGitPath(electivesFile._1), electivesFile._2))

    commitService
      .commit(
        files.toList,
        semester,
        branch
      )
      .map(_ => logger.info("successfully committed files"))
  }

  private def deleteBranch(branch: Branch): Future[Unit] =
    branchApiService
      .deleteBranch(branch)
      .map(_ => logger.info(s"branch ${branch.value} deleted"))

  private def deleteAllFiles(
      files: List[Path],
      pdfFiles: List[ModuleCatalogFile[Path]]
  ): Unit = {
    logger.info("deleting all created files")
    try {
      tmpFolder.deleteContentsOfDirectory()
      files.foreach(p => Files.deleteIfExists(p))
      pdfFiles.foreach(p => Files.deleteIfExists(p.pdfFile))
    } catch {
      case NonFatal(e) =>
        logger.error(s"failed to deleteAllFiles file. reason: ${e.getMessage}")
    }
  }

  private def printAndCompile(semester: Semester) = {
    for {
      sps <- studyProgramViewRepo.all()
      poIds = sps.map(_.poId)
      ms <- moduleRepository.allFromPos(poIds)
      mts <- moduleTypeRepository.all()
      lang <- languageRepository.all()
      seasons <- seasonRepository.all()
      people <- identityRepository.all()
      ams <- assessmentMethodRepository.all()
    } yield {
      def print(sp: StudyProgramView, pLang: PrintingLanguage) = {
        logger.info(s"printing ${sp.fullPoId}...")
        printer.print(
          sp,
          Some(semester),
          ms,
          mts,
          lang,
          seasons,
          people,
          ams,
          sps
        )(pLang)
      }

      def compileAndRecover(
          sp: StudyProgramView,
          pLang: PrintingLanguage,
          content: String,
          filename: String
      ) =
        createTexFile(filename, content) match {
          case Left(err) =>
            logger.error(s"unable to create latex file $filename. reason: $err")
            Left(None, err)
          case Right(texFile) =>
            val compiledRes = for {
              _ <- compile(texFile)
              _ <- clear(texFile)
            } yield logger.info(
              s"compiled ${texFile.toAbsolutePath.toString}"
            )

            compiledRes match {
              case Left(err) =>
                logger.error(
                  s"failed compilation for file ${texFile.toAbsolutePath.toString}. marking as broken..."
                )
                markFileAsBroken(texFile) match {
                  case Left(err) =>
                    Left(Some((texFile)), err)
                  case Right(texFile) =>
                    Left(Some(texFile), err)
                }
              case Right(_) =>
                Right(
                  ModuleCatalogFile[Unit](
                    filename,
                    sp,
                    semester,
                    content,
                    texFile,
                    (),
                    pLang
                  )
                )
            }
        }

      val moduleCatalogFiles = ListBuffer.empty[ModuleCatalogFile[Unit]]
      val brokenFiles = ListBuffer.empty[Path]

      sps.par
        .flatMap(po => PrintingLanguage.all().map(po -> _))
        .map { case (sp, pLang) =>
          val content = print(sp, pLang)
          val filename = s"${pLang.id}_${semester.id}_${sp.fullPoId}"
          compileAndRecover(sp, pLang, content.toString(), filename)
        }
        .seq
        .foreach {
          case Left((path, _)) =>
            path.foreach(brokenFiles += _)
          case Right(file) =>
            moduleCatalogFiles += file
        }

      (moduleCatalogFiles.toList, brokenFiles.toList)
    }
  }

  private def createMergeRequest(implicit
      semester: Semester,
      branch: Branch
  ): Future[(MergeRequestId, MergeRequestStatus)] =
    gitMergeRequestApiService
      .create(
        branch,
        config.mainBranch,
        s"Module Catalog for ${semester.deLabel} ${semester.year}",
        "",
        needsApproval = false,
        config.moduleCatalogLabel
      )
      .map { res =>
        logger.info(
          s"successfully created merge request with id ${res._1.value}"
        )
        res
      }

  private def deleteMergeRequest(mrId: MergeRequestId): Future[Unit] =
    gitMergeRequestApiService
      .delete(mrId)
      .map(_ =>
        logger.info(
          s"successfully deleted mr with id ${mrId.value}"
        )
      )

  private def movePDFsToOutputFolder(
      files: List[ModuleCatalogFile[Unit]]
  ): Future[List[ModuleCatalogFile[Path]]] = {
    logger.info(s"moving ${files.size} pdf files to output folder...")
    val (failure, success) = files.partitionMap { f =>
      val res = for {
        pdf <- getPdf(f.texFile)
        pdf <- movePdf(pdf, f.filename)
      } yield pdf
      res match {
        case Left(err) =>
          logger.error(s"failed to move pdf file of ${f.texFile}. reason: $err")
          Left(f)
        case Right(pdf) =>
          Right(f.copy(pdfFile = pdf))
      }
    }

    if (failure.isEmpty) {
      logger.info("finished moving files!")
      Future.successful(success)
    } else {
      success.foreach(p => Files.deleteIfExists(p.pdfFile))
      val errFiles = failure.map(_.texFile.toAbsolutePath).mkString(", ")
      Future.failed(new Throwable(s"failed moving files: $errFiles"))
    }
  }

  private def createCatalogFiles(xs: Iterable[ModuleCatalogFile[Path]]) = {
    def getPdfFileName(
        xs: Iterable[ModuleCatalogFile[Path]],
        lang: PrintingLanguage
    ): String =
      xs
        .find(_.lang == lang)
        .get
        .pdfFile
        .getFileName
        .toString

    val moduleCatalogFolder =
      Paths.get(config.moduleCatalogOutputFolderPath).getFileName

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
    catalogRepository
      .createOrUpdateMany(normalized)
      .map(res =>
        logger.info(
          s"successfully created ${res.size} module catalogs"
        )
      )
  }

  private def createTexFile(
      name: String,
      content: String
  ): Either[String, Path] = tmpFolder.createFile(s"$name.tex", content)

  private def movePdf(file: Path, newFilename: String): Either[String, Path] =
    try {
      Right(
        Files.move(
          file,
          Paths.get(config.moduleCatalogOutputFolderPath, s"$newFilename.pdf"),
          StandardCopyOption.REPLACE_EXISTING
        )
      )
    } catch {
      case NonFatal(e) => Left(e.getMessage)
    }
}
