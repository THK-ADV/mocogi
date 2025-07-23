package service

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import catalog.Semester
import database.repo.core.SpecializationRepository
import database.view.StudyProgramViewRepository
import git.api.GitDiffApiService
import git.api.GitFileDownloadService
import git.GitConfig
import models.FullPoId
import models.ModuleProtocol
import ops.EitherOps.EStringThrowOps
import parsing.metadata.ModulePOParser
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import printing.latex
import printing.latex.ExamListsLatexPrinter
import service.core.IdentityService
import service.LatexCompiler.compile
import service.LatexCompiler.getPdf

@Singleton
final class ExamListService @Inject() (
    diffApiService: GitDiffApiService,
    downloadService: GitFileDownloadService,
    moduleService: ModuleService,
    studyProgramViewRepo: StudyProgramViewRepository,
    specializationRepository: SpecializationRepository,
    assessmentMethodService: AssessmentMethodService,
    identityService: IdentityService,
    messagesApi: MessagesApi,
    implicit val ctx: ExecutionContext
) extends Logging {

  private given gitConfig: GitConfig = diffApiService.config

  def examLists(po: String, latexFile: Path): Future[Path] =
    generateExamLists(po, latexFile, preview = false)

  def previewExamLists(po: String, latexFile: Path): Future[Path] =
    generateExamLists(po, latexFile, preview = true)

  private def getModules(po: String, preview: Boolean): Future[Seq[ModuleProtocol]] = {
    val liveModules = moduleService.allFromPO(po, activeOnly = false).map(_.map(_._1)) // important: all modules from po
    if preview then {
      for
        liveModules   <- liveModules
        changedModule <- changedActiveModulesFromPreview(po, liveModules.map(_.id.get))
      yield liveModules.filterNot(m => changedModule.exists(_.id == m.id)).appendedAll(changedModule)
    } else {
      liveModules
    }
  }

  private def generateExamLists(po: String, latexFile: Path, preview: Boolean) =
    studyProgramViewRepo.getByPo(FullPoId(po)).flatMap { studyProgram =>
      logger.info(s"generating exam list for po $po (preview = $preview)")

      if studyProgram.specialization.isDefined then
        Future.failed(new Exception("exam list generation is only supported for pos without specialization"))
      else {
        val assessmentMethods = assessmentMethodService.all()
        val people            = identityService.all()
        val specializations   = specializationRepository.allByPO(po)
        val modules           = getModules(po, preview)
        for
          assessmentMethods <- assessmentMethods
          specializations   <- specializations
          people            <- people
          modules           <- modules
          genericModules    <- if specializations.nonEmpty then moduleService.allGeneric() else Future.successful(Nil)
          printer =
            if preview then
              ExamListsLatexPrinter
                .preview(
                  modules,
                  studyProgram,
                  assessmentMethods,
                  people,
                  specializations,
                  genericModules,
                  messagesApi,
                  Lang(Locale.GERMANY)
                )
            else
              ExamListsLatexPrinter
                .default(
                  modules,
                  studyProgram,
                  assessmentMethods,
                  people,
                  specializations,
                  genericModules,
                  Semester.current(),
                  messagesApi,
                  Lang(Locale.GERMANY)
                )
          content = printer
            .print()
            .toString()
          path = Files.writeString(latexFile, content)
          pdf <- compile(path).flatMap(_ => getPdf(path)).toFuture
        yield pdf
      }
    }

  private def changedActiveModulesFromPreview(po: String, liveModules: Seq[UUID]) =
    diffApiService
      .compare(gitConfig.mainBranch, gitConfig.draftBranch)
      .flatMap { diffs =>
        val downloads = diffs.par.collect {
          case d
              if d.path.moduleId.exists(liveModules.contains) ||
                (d.diff.contains(ModulePOParser.modulePOMandatoryKey) ||
                  d.diff.contains(ModulePOParser.modulePOElectiveKey) && d.diff.contains(po)) =>
            downloadService.downloadModuleFromPreviewBranch(d.path.moduleId.get)
        }.seq
        Future.sequence(downloads)
      }
      .map(_.collect {
        case Some(m)
            if m.metadata.po.mandatory.exists(_.po == po) ||
              m.metadata.po.optional.exists(_.po == po) =>
          m
      })
}
