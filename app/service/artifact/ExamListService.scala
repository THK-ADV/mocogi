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
import database.repo.core.AssessmentMethodRepository
import database.repo.core.IdentityRepository
import database.repo.core.SpecializationRepository
import database.repo.ExamListRepository
import database.view.StudyProgramViewRepository
import models.artifact.PublishedDocument
import models.FullPoId
import models.Semester
import ops.toFuture
import ops.FileOps
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import printing.latex
import printing.latex.ExamListsLatexPrinter
import settings.ExamListPathsSettings
import service.ModuleService

@Singleton
final class ExamListService @Inject() (
    moduleService: ModuleService,
    studyProgramViewRepo: StudyProgramViewRepository,
    specializationRepository: SpecializationRepository,
    assessmentMethodRepo: AssessmentMethodRepository,
    identityRepo: IdentityRepository,
    messagesApi: MessagesApi,
    gitCli: GitCLI,
    examListRepo: ExamListRepository,
    paths: ExamListPathsSettings,
    implicit val ctx: ExecutionContext
) extends Logging {

  def currentSemesters(): List[Semester] =
    Semester.currentAndNext()

  def listPublished(): Future[Seq[PublishedDocument]] =
    examListRepo.all()

  def findPublishedFile(filename: String): Option[Path] =
    FileOps.resolvePdfFile(filename, paths.publishedPdfDir)

  def publish(po: String, semester: Semester, date: LocalDate): Future[Unit] = {
    logger.info(s"publishing exam list for po $po")
    for {
      pdf <- TemporaryPdf.generate(s"exam_lists_$po", paths.tmpDir) { latexFile =>
        generateExamList(po, latexFile, Some((semester, date)))
      }
      filename = pdf.publish(s"exam_list_${semester.id}_$po", paths.publishedPdfDir)
      _ <- examListRepo.createOrUpdate(po, semester.id, date, filename)
    } yield ()
  }

  def preview(po: String): Future[TemporaryPdf] = {
    logger.info(s"creating exam list preview for po $po")
    TemporaryPdf.generate(s"exam_lists_$po", paths.tmpDir)(latexFile => generateExamList(po, latexFile, None))
  }

  private def generateExamList(po: String, latexFile: Path, semester: Option[(Semester, LocalDate)]) =
    studyProgramViewRepo.getByPo(FullPoId(po)).flatMap { studyProgram =>
      logger.info(s"generating exam list for po $po (preview = ${semester.isEmpty})")

      if studyProgram.specialization.isDefined then
        Future.failed(new Exception("exam list generation is only supported for pos without specialization"))
      else {
        val assessmentMethods = assessmentMethodRepo.all()
        val people            = identityRepo.list()
        val specializations   = specializationRepository.allByPO(po)
        val lang              = Lang(Locale.GERMANY)

        for
          poModules         <- Future.fromTry(new ModulePreview(gitCli).getByPO(po))
          assessmentMethods <- assessmentMethods
          specializations   <- specializations
          people            <- people
          genericModules    <- if specializations.nonEmpty then moduleService.allGeneric() else Future.successful(Nil)
          modules = poModules.modules.map(_._1)
          printer = semester match {
            case Some((s, d)) =>
              ExamListsLatexPrinter
                .default(
                  modules,
                  poModules.childrenById,
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
                  poModules.childrenById,
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
        yield GeneratedPdf(pdf)
      }
    }
}
