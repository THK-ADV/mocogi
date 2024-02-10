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
import git.api.{GitAvailabilityChecker, GitMergeRequestApiService}
import models._
import ops.EitherOps.EStringThrowOps
import ops.FileOps.FileOps0
import ops.LoggerOps
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
import scala.sys.process._
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
    private val config: ModuleCatalogConfig,
    implicit val ctx: ExecutionContext
) extends Logging
    with LoggerOps {

  import LatexCompiler._

  private def tmpFolder = Paths.get(config.tmpFolderPath)

  def createAndOpenMergeRequest(r: ModuleCatalogGenerationRequest) = {
    val semester = Semester(r.semesterId)
    for {
      (catalogFiles, brokenFiles) <- printAndCompile(semester)
      catalogFiles <- movePDFsToOutputFolder(catalogFiles, brokenFiles).toFuture
      catalogFiles <- moveToGitFolder(catalogFiles, brokenFiles).toFuture
      branch = Branch(semester.id)
      commitMessage = s"Module Catalog for ${semester.deLabel} ${semester.year}"
      res <- commit(branch, commitMessage) match { // TODO commit via cli seems to be flaky. consider using the web api
        case Left(err) =>
          delete(catalogFiles, brokenFiles)
          Future.failed(new Throwable(err))
        case Right(true) =>
          for {
            (mrId, mrStatus) <- createMergeRequest(branch, commitMessage)
              .recoverWith { case NonFatal(e) =>
                delete(catalogFiles, brokenFiles)
                Future.failed(e)
              }
            _ <- createCatalogFiles(catalogFiles)
            _ <- moduleCatalogGenerationRepo.update(mrId, mrStatus, r)
          } yield logger.info(
            s"successfully updated generation request to new id: ${mrId.value} and status ${mrStatus.id}"
          )
        case Right(false) =>
          logger.info("nothing to commit!")
          moduleCatalogGenerationRepo
            .delete(r.mergeRequestId)
            .map(_ =>
              logger.info(
                s"successfully deleted generation request with id ${r.mergeRequestId.value}"
              )
            )
      }
    } yield res
  }

  private def delete(
      catalogFiles: Iterable[ModuleCatalogFile[Path]],
      brokenFiles: Iterable[Path]
  ): Unit = {
    brokenFiles.foreach(a => Files.delete(a))
    catalogFiles.foreach { a =>
      Files.delete(a.texFile)
      Files.delete(a.pdfFile)
    }
  }

  private def printAndCompile(semester: Semester) = {
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
    } yield {
      def print(sp: StudyProgramView, pLang: PrintingLanguage) =
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

      def compileAndRecover(
          sp: StudyProgramView,
          pLang: PrintingLanguage,
          content: StringBuilder,
          filename: String
      ) =
        createTexFile(filename, content) match {
          case Left(err) =>
            logger.error(s"unable to create latex file $filename. reason: $err")
            Left(None, err)
          case Right(texFile) =>
            (for {
              _ <- compile(texFile)
              _ <- clear(texFile)
            } yield ()) match {
              case Left(err) =>
                markFileAsBroken(texFile) match {
                  case Left(err) =>
                    Left(Some(texFile), err)
                  case Right(texFile) =>
                    Left(Some(texFile), err)
                }
              case Right(_) =>
                Right(
                  ModuleCatalogFile[Unit](
                    filename,
                    sp,
                    semester,
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
          compileAndRecover(sp, pLang, content, filename)
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

  private def createMergeRequest(branch: Branch, commitMessage: String) =
    gitMergeRequestApiService
      .create(
        branch,
        config.mainBranch,
        commitMessage,
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

  private def movePDFsToOutputFolder(
      files: Iterable[ModuleCatalogFile[Unit]],
      brokenFiles: Iterable[Path]
  ) = {
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

    if (failure.nonEmpty) {
      logger.error("abort! deleting all files...")
      failure.foreach(a => Files.delete(a.texFile))
      delete(success, brokenFiles)
      val errFiles = failure.map(_.texFile.toAbsolutePath).mkString(", ")
      Left(s"failed moving files: $errFiles")
    } else {
      logger.info("finished moving files!")
      Right(success)
    }
  }

  private def moveToGitFolder(
      files: Iterable[ModuleCatalogFile[Path]],
      brokenFiles: Iterable[Path]
  ): Either[String, Iterable[ModuleCatalogFile[Path]]] = {
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
        case NonFatal(e) =>
          logger.error(
            s"failed to move file from ${src.toAbsolutePath} to ${dest.toAbsolutePath}. reason: ${e.getMessage}"
          )
          Left(f)
      }
    }
    if (failure.nonEmpty) {
      logger.error("abort! deleting all files...")
      failure.foreach { a =>
        Files.delete(a.texFile)
        Files.delete(a.pdfFile)
      }
      delete(success, brokenFiles)
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
}
