package service

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.collection.mutable.ListBuffer
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.*
import database.view.StudyProgramViewRepository
import git.api.GitDiffApiService
import git.api.GitFileDownloadService
import git.GitConfig
import models.core.ModuleStatus
import models.core.Specialization
import models.FullPoId
import models.ModuleCore
import models.ModuleProtocol
import models.StudyProgramView
import ops.EitherOps.EStringThrowOps
import parsing.metadata.ModulePOParser
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import printing.latex.IntroContent
import printing.latex.IntroContentProvider
import printing.latex.ModuleCatalogLatexPrinter
import printing.latex.Payload
import printing.pandoc.PandocApi
import printing.PrintingLanguage
import service.core.IdentityService
import service.modulediff.ModuleProtocolDiff
import service.LatexCompiler.compile
import service.LatexCompiler.getPdf

@Singleton
final class ModulePreviewService @Inject() (
    diffApiService: GitDiffApiService,
    downloadService: GitFileDownloadService,
    moduleService: ModuleService,
    studyProgramViewRepo: StudyProgramViewRepository,
    moduleTypeRepository: ModuleTypeRepository,
    languageRepository: LanguageRepository,
    seasonRepository: SeasonRepository,
    identityService: IdentityService,
    assessmentMethodService: AssessmentMethodService,
    specializationRepository: SpecializationRepository,
    pandocApi: PandocApi,
    messagesApi: MessagesApi,
    @Named("path.mcIntro") mcIntroPath: String,
    implicit val ctx: ExecutionContext
) extends Logging {

  implicit def gitConfig: GitConfig = diffApiService.config

  def previewCatalog(
      fullPoId: FullPoId,
      pLang: PrintingLanguage,
      lang: Lang,
      latexFile: Path
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
      changedModules <- changedActiveModulesFromPreview(
        specialization.fold(fullPoId.id)(identity),
        liveModules.map(_._1.id.get)
      )
      modules = mergeModules(liveModules, changedModules)
      diffs   = diff(liveModules, changedModules)
      intro   = getIntroContent(latexFile.getParent, fullPoId)
      content <- print(studyProgram, modules, studyPrograms, pLang, lang, diffs, intro)
      path = Files.writeString(latexFile, content.toString)
      pdf <- compile(path).flatMap(_ => getPdf(path)).toFuture
    } yield pdf
  }

  private def mergeModules(
      liveModules: Seq[(ModuleProtocol, LocalDateTime)],
      changedModules: Seq[(ModuleProtocol, Option[LocalDateTime])]
  ) = {
    val builder = ListBuffer[(ModuleProtocol, Option[LocalDateTime])]()
    liveModules.foreach {
      case (module, lastModified) =>
        if !changedModules.exists(_._1.id == module.id) then {
          builder.append((module, Some(lastModified)))
        }
    }
    changedModules.foreach(builder.append)
    builder.toList
  }

  private def diff(
      liveModules: Seq[(ModuleProtocol, LocalDateTime)],
      changedModules: Seq[(ModuleProtocol, Option[LocalDateTime])]
  ): Seq[(ModuleCore, Set[String])] =
    changedModules.map {
      case (p, _) =>
        val diffs = liveModules.find(_._1.id == p.id) match {
          case Some((existing, _)) =>
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

  private def changedActiveModulesFromPreview(po: String | Specialization, liveModules: Seq[UUID]) = {
    val poId = po match
      case po: String                  => po
      case Specialization(id, _, _, _) => id

    diffApiService
      .compare(gitConfig.mainBranch, gitConfig.draftBranch)
      .flatMap { diffs =>
        val downloads = diffs.par.collect {
          case d
              if d.path.moduleId.exists(liveModules.contains) ||
                (d.diff.contains(ModulePOParser.modulePOMandatoryKey) && d.diff.contains(poId)) =>
            downloadService.downloadModuleFromPreviewBranchWithLastModified(d.path.moduleId.get)
        }
        Future.sequence(downloads.seq)
      }
      .map(_.collect {
        case Some((m, lastModified)) if ModuleStatus.isActive(m.metadata.status) && m.metadata.po.mandatory.exists {
              a =>
                po match
                  case po: String                   => a.po == po
                  case Specialization(id, _, _, po) => a.po == po && a.specialization.fold(true)(_ == id)
            } =>
          (m, lastModified)
      }.toSeq)
  }

  private def print(
      studyProgram: StudyProgramView,
      modules: List[(ModuleProtocol, Option[LocalDateTime])],
      studyPrograms: Seq[StudyProgramView],
      pLang: PrintingLanguage,
      lang: Lang,
      diffs: Seq[(ModuleCore, Set[String])],
      intro: Option[IntroContent]
  ): Future[StringBuilder] = {
    val liveModules       = moduleService.allModuleCore()
    val createdModules    = moduleService.allNewlyCreated()
    val moduleTypes       = moduleTypeRepository.all()
    val languages         = languageRepository.all()
    val seasons           = seasonRepository.all()
    val people            = identityService.all()
    val assessmentMethods = assessmentMethodService.all()

    for {
      liveModules       <- liveModules
      createdModules    <- createdModules
      moduleTypes       <- moduleTypes
      languages         <- languages
      seasons           <- seasons
      people            <- people
      assessmentMethods <- assessmentMethods
    } yield {
      val payload = Payload(
        studyProgram,
        moduleTypes,
        languages,
        seasons,
        people,
        assessmentMethods,
        studyPrograms,
        liveModules ++ createdModules
      )
      val printer = ModuleCatalogLatexPrinter.preview(
        pandocApi,
        messagesApi,
        diffs,
        intro,
        modules,
        payload,
        pLang,
        lang,
      )
      printer.print()
    }
  }

  private def getIntroContent(dir: Path, fullPoId: FullPoId) = {
    val provider = IntroContentProvider(dir, fullPoId, mcIntroPath)
    provider.createIntroContent()
  }
}
