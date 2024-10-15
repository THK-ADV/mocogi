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

import database.view.StudyProgramViewRepository
import git.api.GitDiffApiService
import git.api.GitFileDownloadService
import git.GitConfig
import models.FullPoId
import ops.EitherOps.EStringThrowOps
import parsing.metadata.ModulePOParser
import play.api.i18n.Lang
import play.api.libs.Files.TemporaryFile
import play.api.Logging
import printing.latex.ExamListsLatexPrinter
import service.core.AssessmentMethodService
import service.core.IdentityService
import service.LatexCompiler.compile
import service.LatexCompiler.getPdf

@Singleton
final class ExamListsPreviewService @Inject() (
    diffApiService: GitDiffApiService,
    downloadService: GitFileDownloadService,
    moduleService: ModuleService,
    printer: ExamListsLatexPrinter,
    studyProgramViewRepo: StudyProgramViewRepository,
    assessmentMethodService: AssessmentMethodService,
    identityService: IdentityService,
    implicit val ctx: ExecutionContext
) extends Logging {

  given gitConfig: GitConfig = diffApiService.config
  given Lang(Locale.GERMANY)

  def previewExamLists(fullPoId: FullPoId, latexFile: TemporaryFile): Future[Path] = {
    logger.info(s"generating preview for po ${fullPoId.id}")

    val studyProgram      = studyProgramViewRepo.getByPo(fullPoId)
    val liveModules       = moduleService.allFromPo(fullPoId.id)
    val assessmentMethods = assessmentMethodService.all()
    val people            = identityService.all()

    for
      studyProgram      <- studyProgram
      liveModules       <- liveModules
      assessmentMethods <- assessmentMethods
      people            <- people
      changedModule     <- changedModuleFromPreview(fullPoId.id, liveModules.map(_.id.get))
      modules = liveModules.filterNot(m => changedModule.exists(_.id == m.id)).appendedAll(changedModule)
      content = printer.preview(modules, studyProgram, assessmentMethods, people)
      path    = Files.writeString(latexFile, content.toString)
      pdf <- compile(path).flatMap(_ => getPdf(path)).toFuture
    yield pdf
  }

  private def changedModuleFromPreview(poId: String, liveModules: Seq[UUID]) =
    diffApiService
      .compare(gitConfig.mainBranch, gitConfig.draftBranch)
      .flatMap { diffs =>
        val downloads = diffs.par.collect {
          case d
              if d.path.moduleId.exists(liveModules.contains) ||
                d.diff.contains(ModulePOParser.modulePOMandatoryKey) ||
                d.diff.contains(ModulePOParser.modulePOElectiveKey) =>
            downloadService.downloadModuleFromPreviewBranch(d.path.moduleId.get)
        }.seq
        Future.sequence(downloads)
      }
      .map(_.collect {
        case Some(m)
            if m.metadata.po.mandatory.exists(_.po == poId) ||
              m.metadata.po.optional.exists(_.po == poId) =>
          m
      })
}
