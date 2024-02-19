package service

import database.repo.core._
import database.view.StudyProgramViewRepository
import git.GitConfig
import git.api.{GitDiffApiService, GitFileDownloadService}
import models.{ModuleCore, ModuleProtocol, StudyProgramView}
import ops.EitherOps.EStringThrowOps
import parsing.metadata.ModulePOParser
import play.api.Logging
import play.api.libs.Files.TemporaryFile
import printing.PrintingLanguage
import printing.latex.ModuleCatalogLatexPrinter
import service.LatexCompiler.{compile, getPdf}

import java.nio.file.{Files, Path}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModulePreviewService @Inject() (
    diffApiService: GitDiffApiService,
    downloadService: GitFileDownloadService,
    printer: ModuleCatalogLatexPrinter,
    moduleService: ModuleService,
    studyProgramViewRepo: StudyProgramViewRepository,
    moduleTypeRepository: ModuleTypeRepository,
    languageRepository: LanguageRepository,
    seasonRepository: SeasonRepository,
    identityRepository: IdentityRepository,
    assessmentMethodRepository: AssessmentMethodRepository,
    implicit val ctx: ExecutionContext
) extends Logging {

  implicit def gitConfig: GitConfig = diffApiService.config

  def previewCatalog(
      poId: String,
      pLang: PrintingLanguage,
      latexFile: TemporaryFile
  ): Future[Path] = {
    val studyPrograms = studyProgramViewRepo.all()

    for {
      studyPrograms <- studyPrograms
      studyProgram <- studyPrograms.find(_.po.id == poId) match {
        case Some(value) =>
          Future.successful(value)
        case None =>
          Future.failed(
            new Throwable(s"study program's po $poId needs to be valid")
          )
      }
      liveModules <- moduleService.allFromPoMandatory(poId)
      previewModules <- getPreviewModules(poId, liveModules.map(_.id.get))
      modules = liveModules
        .filterNot(m => previewModules.exists(_.id == m.id))
        .appendedAll(previewModules)
      diffs = diff(liveModules, previewModules)
      content <- print(studyProgram, modules, studyPrograms, pLang, diffs)
      path = Files.writeString(latexFile, content.toString)
      pdf <- compile(path).flatMap(_ => getPdf(path)).toFuture
    } yield pdf
  }

  private def diff(
      liveModules: Seq[ModuleProtocol],
      previewModule: Seq[ModuleProtocol]
  ): Seq[(ModuleCore, Set[String])] =
    previewModule.map { p =>
      val diffs = liveModules.find(_.id == p.id) match {
        case Some(existing) =>
          val (_, diffs) = ModuleProtocolDiff.diff(
            existing.normalize(),
            p.normalize(),
            None,
            Set.empty
          )
          diffs
        case None =>
          Set("all")
      }
      (ModuleCore(p.id.get, p.metadata.title, p.metadata.abbrev), diffs)
    }

  private def getPreviewModules(poId: String, liveModules: Seq[UUID]) =
    diffApiService
      .compare(gitConfig.mainBranch, gitConfig.draftBranch)
      .flatMap { diffs =>
        val downloads = diffs
          .collect {
            case d
                if d.path.moduleId.exists(liveModules.contains) ||
                  d.diff.contains(ModulePOParser.modulePOMandatoryKey) =>
              downloadService
                .downloadModuleFromPreviewBranch(d.path.moduleId.get)
          }
        Future.sequence(downloads)
      }
      .map(_.collect {
        case Some(m) if m.metadata.po.mandatory.exists(_.po == poId) => m
      })

  private def print(
      studyProgram: StudyProgramView,
      modules: Seq[ModuleProtocol],
      studyPrograms: Seq[StudyProgramView],
      pLang: PrintingLanguage,
      diffs: Seq[(ModuleCore, Set[String])]
  ): Future[StringBuilder] =
    for {
      mts <- moduleTypeRepository.all()
      lang <- languageRepository.all()
      seasons <- seasonRepository.all()
      people <- identityRepository.all()
      ams <- assessmentMethodRepository.all()
    } yield printer.print(
      studyProgram,
      None,
      modules,
      mts,
      lang,
      seasons,
      people,
      ams,
      studyPrograms,
      diffs
    )(pLang)
}
