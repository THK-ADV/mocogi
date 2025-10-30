package service

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.SpecializationRepository
import database.view.StudyProgramViewRepository
import git.api.GitDiffApiService
import git.api.GitFileDownloadService
import git.GitConfig
import models.FullPoId
import models.ModuleProtocol
import models.Semester
import ops.EitherOps.EStringThrowOps
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

  def createExamList(po: String, latexFile: Path, semester: Semester, date: LocalDate): Future[Path] =
    generateExamList(po, latexFile, Some((semester, date)))

  def previewExamList(po: String, latexFile: Path): Future[Path] =
    generateExamList(po, latexFile, None)

  private def getModules(po: String): Future[Seq[ModuleProtocol]] = {
    val previewService = new ModulePreview(diffApiService, downloadService, ctx)
    val liveModules    = moduleService.allFromPO(po, activeOnly = true).map(_.map(_._1))
    for
      liveModules <- liveModules
      modules     <- previewService.mergeWithChangedModulesFromPreview(po, liveModules)
    yield modules
  }

  private def generateExamList(po: String, latexFile: Path, semester: Option[(Semester, LocalDate)]) =
    studyProgramViewRepo.getByPo(FullPoId(po)).flatMap { studyProgram =>
      logger.info(s"generating exam list for po $po (preview = ${semester.isEmpty})")

      if studyProgram.specialization.isDefined then
        Future.failed(new Exception("exam list generation is only supported for pos without specialization"))
      else {
        val assessmentMethods = assessmentMethodService.all()
        val people            = identityService.all()
        val specializations   = specializationRepository.allByPO(po)
        val modules           = getModules(po)
        val lang              = Lang(Locale.GERMANY)

        for
          assessmentMethods <- assessmentMethods
          specializations   <- specializations
          people            <- people
          modules           <- modules
          genericModules    <- if specializations.nonEmpty then moduleService.allGeneric() else Future.successful(Nil)
          printer = semester match {
            case Some((s, d)) =>
              ExamListsLatexPrinter
                .default(
                  modules,
                  studyProgram,
                  assessmentMethods,
                  people,
                  specializations,
                  genericModules,
                  s,
                  d,
                  messagesApi,
                  lang
                )
            case None =>
              ExamListsLatexPrinter
                .preview(
                  modules,
                  studyProgram,
                  assessmentMethods,
                  people,
                  specializations,
                  genericModules,
                  messagesApi,
                  lang
                )
          }
          content = printer.print().toString()
          path    = Files.writeString(latexFile, content)
          pdf <- compile(path).flatMap(_ => getPdf(path)).toFuture
        yield pdf
      }
    }
}
