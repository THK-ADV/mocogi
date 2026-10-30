package service.artifact.examlist

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure

import cats.data.EitherNel
import cats.syntax.either.*
import cats.syntax.option.*
import cats.syntax.parallel.*
import cats.syntax.traverse.*
import cli.LatexCompiler.compile
import cli.LatexCompiler.getPdf
import database.repo.ExamListRepository
import models.*
import models.core.ExamPhases.ExamPhase
import ops.toFuture
import ops.FileOps
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.libs.json.JsValue
import play.api.Logging
import printing.latex.ExamListsLatexPrinter
import service.artifact.GeneratedPdf
import service.artifact.TemporaryPdf

final class ExamListConfigException(message: String) extends IllegalArgumentException(message)

object ExamListService {

  /**
   * Applies the validated configuration to the candidates and resolves the registration period of each exam and phase.
   * Orphaned exclusions and overrides do not match any candidate and are thus ignored.
   *
   * @return all missing registration periods, or the included entries in the order of the candidates
   */
  def prepare(
      config: ExamListConfig,
      options: Seq[ExamListModuleOption]
  ): Either[String, List[PreparedExamListEntry]] = {
    val excludedModules = config.excludedModuleIds.toSet
    val excludedExams   = config.excludedExams.toSet
    val overrides       =
      config.registrationOverrides.map(o => (o.module, o.exam, o.phase) -> DateRange(o.start, o.end)).toMap

    def period(m: ExamListModuleOption, exam: Option[String], phase: String) =
      overrides
        .get((m.id, exam, phase))
        .orElse(config.registrationPeriods.get(phase))
        .collect { case DateRange(Some(start), Some(end)) => phase -> ExamListPeriod(start, end) }
        .toRightNel(
          s"missing registration period for module ${m.title} (${m.id}), exam ${exam.getOrElse(m.title)}, phase $phase"
        )

    def entry(m: ExamListModuleOption): Option[EitherNel[String, PreparedExamListEntry]] =
      if m.isGeneric then Some(Right(PreparedExamListEntry(m, m.examPhases.filterNot(_ == ExamPhase.none.id), Nil)))
      else {
        val phases = m.examPhases.filter(config.examPhases.contains)
        val exams  = m.examNames.fold(List(None))(_.map(Some(_))).filterNot(e => excludedExams(ExamRef(m.id, e)))
        Option.when(phases.nonEmpty && exams.nonEmpty)(
          exams
            .parTraverse(e => phases.parTraverse(period(m, e, _)).map(ps => ExamListExam(e, ps.toMap)))
            .map(PreparedExamListEntry(m, phases, _))
        )
      }

    options.toList
      .filterNot(m => config.excludeGenericModules && m.isGeneric || excludedModules(m.id))
      .flatMap(entry)
      .parSequence
      .leftMap(_.toList.mkString("; "))
  }
}

@Singleton
final class ExamListService @Inject() (
    examListRepo: ExamListRepository,
    messagesApi: MessagesApi,
    pdfConfig: ExamListPdfConfig,
    implicit val ctx: ExecutionContext
) extends Logging {

  /** Returns the current and next semester and those of existing lists in chronological order. */
  def semesters(): Future[List[Semester]] =
    examListRepo.semesters().map(ids => (Semester.currentAndNext() ++ ids.map(Semester.apply)).distinctBy(_.id).sorted)

  /**
   * Returns the published lists with their study program and PDF filename, by default only the latest semester of each
   * PO. An explicit semester returns the lists of that semester.
   */
  def listPublished(po: Option[String], semester: Option[String], latestOnly: Boolean): Future[Seq[ExamList]] =
    examListRepo.all(po, semester, latestOnly)

  /**
   * Returns the module rows stored with the list, the data basis of exam scheduling (feature 3), or `None` if the list
   * does not exist.
   */
  def entries(id: UUID): Future[Option[Seq[ExamListEntry]]] = examListRepo.entries(id)

  /**
   * Returns all candidates of the PO from the live modules, including generic and currently filtered ones. The frontend
   * builds the configuration from them. Preview and publication read the same candidates again and apply the
   * configuration to them.
   *
   * @return `None` if the PO has no current study program
   */
  def configOptions(po: String): Future[Option[Seq[ExamListModuleOption]]] = examListRepo.options(po)

  /**
   * Returns the stored configuration of the PO unchanged as the starting point of the next preview or publication.
   *
   * @return `None` if the PO does not exist, `Some(None)` if nothing is stored yet
   */
  def config(po: String): Future[Option[Option[JsValue]]] = examListRepo.config(po)

  /**
   * Replaces the stored configuration of the PO, which is shared by all its editors. Preview and publication do not
   * save it, they use the configuration they are given.
   *
   * @return `false` if the PO does not exist
   */
  def saveConfig(po: String, config: ExamListConfig): Future[Boolean] = examListRepo.saveConfig(po, config)

  /** Resolves the filename of a published list, as referenced by its url, to the archived PDF. */
  def findPublishedFile(filename: String): Option[Path] = FileOps.resolvePdfFile(filename, pdfConfig.publishedPdfDir)

  /**
   * Generates the PDF of the given configuration with a watermark, without storing anything.
   *
   * @return `None` if the PO has no current study program
   */
  def preview(po: String, config: ExamListConfig): Future[Option[TemporaryPdf]] = {
    logger.info(s"creating exam list preview for po $po")
    generate(po, config, isPreview = true).map(_.map(_._1))
  }

  /**
   * Publishes the list of the given configuration for its semester, as previewed. The PDF is archived and the stored
   * list of the PO and semester is replaced by the included modules, keeping the ids of the list and retained rows. If
   * the replacement fails, the new PDF is removed, so the previous list and its PDF stay valid.
   *
   * @return `false` if the PO has no current study program
   */
  def publish(po: String, config: ExamListConfig): Future[Boolean] =
    config.date match {
      case None       => Future.failed(ExamListConfigException("release date is required"))
      case Some(date) =>
        logger.info(s"publishing exam list for po $po and semester ${config.semester}")
        generate(po, config, isPreview = false).flatMap(_.traverse { (pdf, entries) =>
          val filename = pdf.publish(s"exam_list_${config.semester}_$po", pdfConfig.publishedPdfDir)
          examListRepo.replace(po, config.semester, date, filename, entries).andThen {
            case Failure(_) => Files.deleteIfExists(Paths.get(pdfConfig.publishedPdfDir).resolve(filename))
          }
        }.map(_.isDefined))
    }

  private def generate(po: String, config: ExamListConfig, isPreview: Boolean) =
    examListRepo
      .data(po)
      .flatMap(_.traverse { data =>
        ExamListService.prepare(config, data.options) match {
          case Left(error)    => Future.failed(ExamListConfigException(error))
          case Right(entries) =>
            TemporaryPdf
              .generate(s"exam_lists_$po", pdfConfig.tmpDir) { latexFile =>
                val printer =
                  new ExamListsLatexPrinter(
                    entries,
                    data,
                    Semester(config.semester),
                    config.date,
                    isPreview,
                    messagesApi
                  )(
                    using Lang(Locale.GERMANY)
                  )
                val path = Files.writeString(latexFile, printer.print().toString)
                compile(path).flatMap(_ => getPdf(path)).toFuture.map(GeneratedPdf(_))
              }
              .map(_ -> entries)
        }
      })
}
