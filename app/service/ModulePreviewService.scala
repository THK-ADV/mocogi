package service

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.collection.mutable.ListBuffer
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import cats.data.NonEmptyList
import database.repo.core.*
import database.view.StudyProgramViewRepository
import git.api.GitDiffApiService
import git.api.GitFileDownloadService
import git.GitConfig
import models.core.ModuleStatus
import models.ModuleCore
import models.ModuleProtocol
import models.StudyProgramView
import ops.EitherOps.EStringThrowOps
import ops.FileOps.FileOps0
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import printing.latex.snippet.DiffContentSnippet
import printing.latex.snippet.IntroContentProvider
import printing.latex.snippet.LatexContentSnippet
import printing.latex.snippet.LayoutContentSnippet
import printing.latex.ModuleCatalogLatexPrinter
import printing.latex.Payload
import printing.pandoc.PandocApi
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
    poRepository: PORepository,
    pandocApi: PandocApi,
    messagesApi: MessagesApi,
    @Named("path.mcIntro") mcIntroPath: String,
    @Named("path.mcAssets") mcAssetsPath: String,
    implicit val ctx: ExecutionContext
) extends Logging {

  implicit def gitConfig: GitConfig = diffApiService.config

  private type ModuleDiffs = List[(ModuleCore, Set[String])]

  def previewCatalog(po: String, lang: Lang, latexFile: Path): Future[Path] = {
    logger.info(s"generating module catalog preview for po $po")

    val studyPrograms = studyProgramViewRepo.notExpired().map { all =>
      val poOnly = all.filter(_.po.id == po)
      assume(poOnly.nonEmpty, s"expected study programs for po $po")
      (all, poOnly)
    }

    for {
      (all, poOnly)  <- studyPrograms
      liveModules    <- moduleService.allFromPO(po, activeOnly = true)
      changedModules <- changedActiveModulesFromPreview(po, liveModules.map(_._1.id.get))
      modules       = mergeModules(liveModules, changedModules)
      moduleDiffs   = diffs(liveModules, changedModules)
      latexSnippets = getLatexSnippets(latexFile.getParent, po, moduleDiffs)
      _             = copyAssets(latexFile.getParent)
      content <- print(poOnly, modules, all, lang, moduleDiffs, latexSnippets)
      path = Files.writeString(latexFile, content.toString)
      pdf <- compile(path).flatMap(_ => getPdf(path)).toFuture
    } yield pdf
  }

  private def mergeModules(
      liveModules: Seq[(ModuleProtocol, LocalDateTime)],
      changedModules: Seq[(ModuleProtocol, Option[LocalDateTime])]
  ) = {
    val builder = ListBuffer[(ModuleProtocol, Option[LocalDateTime])](changedModules*)
    liveModules.foreach {
      case (liveModule, lastModified) =>
        if !builder.exists(_._1.id.get == liveModule.id.get) then {
          builder.append((liveModule, Some(lastModified)))
        }
    }
    assume(
      builder.size == builder.distinctBy(_._1.id.get).size,
      s"""expected modules to be unique
         |liveModules: ${liveModules.map(_._1.id.get)}
         |changedModules: ${changedModules.map(_._1.id.get)}
         |builder: ${builder.map(_._1.id.get)}""".stripMargin
    )
    builder.toList
  }

  private def diffs(
      liveModules: Seq[(ModuleProtocol, LocalDateTime)],
      changedModules: Seq[(ModuleProtocol, Option[LocalDateTime])]
  ): ModuleDiffs =
    changedModules.foldLeft(Nil) {
      case (acc, (p, _)) =>
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
        acc.prepended((ModuleCore(p.id.get, p.metadata.title, p.metadata.abbrev), diffs))
    }

  private def changedActiveModulesFromPreview(po: String, liveModules: Seq[UUID]) =
    diffApiService
      .compare(gitConfig.mainBranch, gitConfig.draftBranch)
      .flatMap { diffs =>
        val downloads =
          diffs.par.map(d => downloadService.downloadModuleFromPreviewBranchWithLastModified(d.path.moduleId.get))
        Future.sequence(downloads.seq)
      }
      .map(_.collect {
        case Some((m, lastModified))
            if ModuleStatus.isActive(m.metadata.status) && (m.metadata.po.mandatory
              .exists(a => a.po == po) || m.metadata.po.optional.exists(a => a.po == po)) =>
          (m, lastModified)
      }.toSeq)

  private def print(
      poOnly: Seq[StudyProgramView],
      modules: List[(ModuleProtocol, Option[LocalDateTime])],
      studyPrograms: Seq[StudyProgramView],
      lang: Lang,
      diffs: ModuleDiffs,
      latexSnippets: List[LatexContentSnippet]
  ): Future[StringBuilder] = {
    val liveModules       = moduleService.allModuleCore()
    val createdModules    = moduleService.allNewlyCreated()
    val moduleTypes       = moduleTypeRepository.all()
    val languages         = languageRepository.all()
    val seasons           = seasonRepository.all()
    val people            = identityService.all()
    val assessmentMethods = assessmentMethodService.all()
    val currentPO         = poRepository.get(poOnly.head.po.id)

    for {
      liveModules       <- liveModules
      createdModules    <- createdModules
      moduleTypes       <- moduleTypes
      languages         <- languages
      seasons           <- seasons
      people            <- people
      assessmentMethods <- assessmentMethods
      currentPO         <- currentPO
    } yield {
      val payload = Payload(
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
        id => diffs.find(_._1.id == id).map(_._2),
        latexSnippets,
        poOnly,
        currentPO,
        modules,
        payload,
        lang,
      )
      printer.print()
    }
  }

  // TODO: same for prod catalog
  private def copyAssets(parentDir: Path): Unit =
    try {
      Paths.get(mcAssetsPath).foreachFileOfDirectory { path =>
        path.copy(parentDir).match {
          case Left(err) => throw Exception(s"failed to copy assets into media folder: $err")
          case Right(_)  =>
        }
      }
    } catch {
      case NonFatal(e) => throw Exception(e)
    }

  private def getLatexSnippets(dir: Path, po: String, moduleDiffs: ModuleDiffs): List[LatexContentSnippet] = {
    val introContent = IntroContentProvider(dir, po, mcIntroPath).createIntroContent()
    val diffContent  = NonEmptyList.fromList(moduleDiffs).map(DiffContentSnippet(_, messagesApi))
    val layout       = LayoutContentSnippet()
    layout :: List(diffContent, introContent).collect { case Some(snippet) => snippet }
  }
}
