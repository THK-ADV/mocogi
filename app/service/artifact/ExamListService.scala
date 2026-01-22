package service.artifact

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cli.GitCLI
import cli.LatexCompiler.compile
import cli.LatexCompiler.getPdf
import database.repo.core.SpecializationRepository
import database.view.StudyProgramViewRepository
import models.FullPoId
import models.ModuleProtocol
import models.Semester
import ops.toFuture
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import printing.latex
import printing.latex.ExamListsLatexPrinter
import service.core.AssessmentMethodService
import service.core.IdentityService
import service.ModuleService

@Singleton
final class ExamListService @Inject() (
    moduleService: ModuleService,
    studyProgramViewRepo: StudyProgramViewRepository,
    specializationRepository: SpecializationRepository,
    assessmentMethodService: AssessmentMethodService,
    identityService: IdentityService,
    messagesApi: MessagesApi,
    gitCli: GitCLI,
    implicit val ctx: ExecutionContext
) extends Logging {

  def createExamList(po: String, latexFile: Path, semester: Semester, date: LocalDate): Future[Path] =
    generateExamList(po, latexFile, Some((semester, date)))

  def previewExamList(po: String, latexFile: Path): Future[Path] =
    generateExamList(po, latexFile, None)

  private def getModulesFromPreview(po: String): Vector[ModuleProtocol] = {
    val previewService = new ModulePreview(gitCli)
    previewService.getAllFromPreviewByPO(po)
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
        val modules           = getModulesFromPreview(po)
        val lang              = Lang(Locale.GERMANY)

        for
          assessmentMethods <- assessmentMethods
          specializations   <- specializations
          people            <- people
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
