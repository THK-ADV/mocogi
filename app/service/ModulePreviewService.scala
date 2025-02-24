package service

import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.*
import database.view.StudyProgramViewRepository
import git.api.GitDiffApiService
import git.api.GitFileDownloadService
import git.GitConfig
import models.core.Specialization
import models.FullPoId
import models.ModuleCore
import models.ModuleProtocol
import models.StudyProgramView
import ops.EitherOps.EStringThrowOps
import parsing.metadata.ModulePOParser
import play.api.i18n.Lang
import play.api.libs.Files.TemporaryFile
import play.api.Logging
import printing.latex.IntroContent
import printing.latex.IntroContentProvider
import printing.latex.ModuleCatalogLatexPrinter
import printing.latex.Payload
import printing.PrintingLanguage
import service.modulediff.ModuleProtocolDiff
import service.LatexCompiler.compile
import service.LatexCompiler.getPdf

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
    assessmentMethodService: AssessmentMethodService,
    specializationRepository: SpecializationRepository,
    @Named("path.mcIntro") mcIntroPath: String,
    implicit val ctx: ExecutionContext
) extends Logging {

  implicit def gitConfig: GitConfig = diffApiService.config

  def previewCatalog(
      fullPoId: FullPoId,
      pLang: PrintingLanguage,
      lang: Lang,
      latexFile: TemporaryFile
  ): Future[Path] = {
    logger.info(s"generating module catalog preview for po ${fullPoId.id}")

    val studyPrograms  = studyProgramViewRepo.all()
    val specialization = specializationRepository.get(fullPoId.id)

    for {
      studyPrograms  <- studyPrograms
      specialization <- specialization
      studyProgram <- studyPrograms.find(_.fullPoId == fullPoId) match {
        case Some(value) =>
          Future.successful(value)
        case None =>
          Future.failed(
            new Exception(s"study program's po $fullPoId needs to be valid")
          )
      }
      liveModules <- moduleService.allFromMandatoryPO(specialization.fold(fullPoId.id)(identity))
      changedModule <- changedModuleFromPreview(
        specialization.fold(fullPoId.id)(identity),
        liveModules.map(_.id.get)
      )
      modules = liveModules
        .filterNot(m => changedModule.exists(_.id == m.id))
        .appendedAll(changedModule)
      diffs = diff(liveModules, changedModule.toSeq)
      intro = getIntroContent(latexFile.path.getParent, fullPoId)
      content <- print(studyProgram, modules, studyPrograms, pLang, lang, diffs, intro)
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

  private def changedModuleFromPreview(po: String | Specialization, liveModules: Seq[UUID]) = {
    val poId = po match
      case po: String               => po
      case Specialization(id, _, _) => id

    diffApiService
      .compare(gitConfig.mainBranch, gitConfig.draftBranch)
      .flatMap { diffs =>
        val downloads = diffs.par.collect {
          case d
              if d.path.moduleId.exists(liveModules.contains) ||
                (d.diff.contains(ModulePOParser.modulePOMandatoryKey) && d.diff.contains(poId)) =>
            downloadService
              .downloadModuleFromPreviewBranch(d.path.moduleId.get)
        }
        Future.sequence(downloads.seq)
      }
      .map(_.collect {
        case Some(m) if m.metadata.po.mandatory.exists { a =>
              po match
                case po: String                => a.po == po
                case Specialization(id, _, po) => a.po == po && a.specialization.fold(true)(_ == id)
            } =>
          m
      })
  }

  private def print(
      studyProgram: StudyProgramView,
      modules: Seq[ModuleProtocol],
      studyPrograms: Seq[StudyProgramView],
      pLang: PrintingLanguage,
      lang: Lang,
      diffs: Seq[(ModuleCore, Set[String])],
      intro: Option[IntroContent]
  ): Future[StringBuilder] =
    for {
      mts     <- moduleTypeRepository.all()
      langs   <- languageRepository.all()
      seasons <- seasonRepository.all()
      people  <- identityRepository.all()
      ams     <- assessmentMethodService.all()
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
      lang,
      intro
    )

  private def getIntroContent(dir: Path, fullPoId: FullPoId) = {
    val provider = IntroContentProvider(dir, fullPoId, mcIntroPath)
    provider.createIntroContent()
  }
}
