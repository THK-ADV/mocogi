package service

import database.repo.core._
import database.view.StudyProgramViewRepository
import git.GitConfig
import git.api.{GitDiffApiService, GitFileDownloadService}
import models.{FullPoId, ModuleCore, ModuleProtocol, StudyProgramView}
import ops.EitherOps.EStringThrowOps
import parsing.metadata.ModulePOParser
import play.api.Logging
import play.api.i18n.Lang
import play.api.libs.Files.TemporaryFile
import printing.PrintingLanguage
import printing.latex.{ModuleCatalogLatexPrinter, Payload}
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
      fullPoId: FullPoId,
      pLang: PrintingLanguage,
      lang: Lang,
      latexFile: TemporaryFile
  ): Future[Path] = {
    val studyPrograms = studyProgramViewRepo.all()

    for {
      studyPrograms <- studyPrograms
      studyProgram <- studyPrograms.find(_.fullPoId == fullPoId) match {
        case Some(value) =>
          Future.successful(value)
        case None =>
          Future.failed(
            new Exception(s"study program's po $fullPoId needs to be valid")
          )
      }
      liveModules <- moduleService.allFromPoMandatory(fullPoId.id)
      changedModule <- changedModuleFromPreview(
        fullPoId.id,
        liveModules.map(_.id.get)
      )
      modules = liveModules
        .filterNot(m => changedModule.exists(_.id == m.id))
        .appendedAll(changedModule)
      diffs = diff(liveModules, changedModule)
      content <- print(studyProgram, modules, studyPrograms, pLang, lang, diffs)
      path = Files.writeString(latexFile, content.toString)
      pdf <- compile(path).flatMap(_ => getPdf(path)).toFuture
    } yield pdf
  }

  private def diff(
      liveModules: Seq[ModuleProtocol],
      changedModules: Seq[ModuleProtocol]
  ): Seq[(ModuleCore, Set[String])] =
    changedModules.map { p =>
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

  private def changedModuleFromPreview(poId: String, liveModules: Seq[UUID]) =
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
      lang: Lang,
      diffs: Seq[(ModuleCore, Set[String])]
  ): Future[StringBuilder] =
    for {
      mts <- moduleTypeRepository.all()
      langs <- languageRepository.all()
      seasons <- seasonRepository.all()
      people <- identityRepository.all()
      ams <- assessmentMethodRepository.all()
    } yield printer.preview(
      diffs,
      Payload(
        studyProgram,
        modules,
        mts,
        langs,
        seasons,
        people,
        ams,
        studyPrograms
      ),
      pLang,
      lang
    )
}
