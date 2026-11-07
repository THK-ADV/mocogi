package service.artifact.examlist

import java.time.LocalDate
import java.util.UUID

import models.core.AssessmentMethod
import models.core.ExamPhases.ExamPhase
import models.core.Identity
import models.core.Specialization
import models.ModuleCore
import models.Semester
import models.StudyProgramView
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OptionHandlers
import play.api.libs.json.Writes

private val nullable = Json.configured(JsonConfiguration(optionHandlers = OptionHandlers.WritesNull))

/** Inclusive date range whose bounds may be missing while the configuration is edited. */
final case class DateRange(start: Option[LocalDate], end: Option[LocalDate])

/** References an exam of a module. An exam assigned to several modules has one reference per module. */
final case class ExamRef(module: UUID, exam: UUID)

final case class RegistrationOverride(
    module: UUID,
    exam: UUID,
    phase: String,
    start: Option[LocalDate],
    end: Option[LocalDate]
)

/** Shared configuration of the exam list of a PO. */
final case class ExamListConfig(
    semester: String,
    date: Option[LocalDate],
    examPhases: List[String],
    excludeGenericModules: Boolean,
    excludedModuleIds: List[UUID],
    excludedExams: List[ExamRef],
    registrationPeriods: Map[String, DateRange],
    registrationOverrides: List[RegistrationOverride]
)

object ExamListConfig {
  given Format[DateRange]            = nullable.format
  given Format[ExamRef]              = nullable.format
  given Format[RegistrationOverride] = nullable.format
  given Format[ExamListConfig]       = nullable.format

  private val phases = ExamPhase.all.toList.map(_.id).toSet - ExamPhase.none.id

  /** Returns the errors that prevent saving. Incomplete periods and orphaned entries are allowed. */
  def errors(c: ExamListConfig): List[String] = {
    def reversed(range: String, start: Option[LocalDate], end: Option[LocalDate]) =
      start.zip(end).collect { case (s, e) if s.isAfter(e) => s"$range starts after it ends" }
    def overrideLabel(o: RegistrationOverride) =
      s"registration override for module ${o.module}, exam ${o.exam}, phase ${o.phase}"

    Option.unless(c.semester.matches("(sose|wise)_\\d{4}"))(s"invalid semester ${c.semester}").toList ++
      c.examPhases.filterNot(phases).map(p => s"unknown exam phase $p") ++
      c.registrationPeriods.keys.filterNot(phases).map(p => s"unknown exam phase $p of a registration period") ++
      c.registrationPeriods.flatMap((p, r) => reversed(s"registration period of phase $p", r.start, r.end)) ++
      c.registrationOverrides.filterNot(o => phases(o.phase)).map(o => s"${overrideLabel(o)} has an unknown phase") ++
      c.registrationOverrides.flatMap(o => reversed(overrideLabel(o), o.start, o.end)) ++
      c.registrationOverrides.groupBy(o => (o.module, o.exam, o.phase)).collect {
        case (_, o :: _ :: _) => s"${overrideLabel(o)} is defined more than once"
      }
  }
}

/** PO relation of a module within a PO. `instanceOf` is the generic module of an elective relation. */
final case class ExamListPO(
    po: String,
    specialization: Option[String],
    recommendedSemester: List[Int],
    mandatory: Boolean,
    instanceOf: Option[UUID]
)

object ExamListPO {
  given Format[ExamListPO] = nullable.format
}

final case class ExamListAssessmentMethod(method: String, percentage: Option[Double])

final case class ExamListModuleExam(id: UUID, name: String)

/**
 * Active candidate module of an exam list with its relations united over its child modules. `semester` is the smallest
 * recommended semester of these relations, or `Int.MaxValue` without any.
 */
final case class ExamListModuleOption(
    id: UUID,
    title: String,
    abbrev: String,
    isGeneric: Boolean,
    mandatory: Boolean,
    semester: Int,
    po: List[ExamListPO],
    assessmentMethods: List[ExamListAssessmentMethod],
    firstExaminer: String,
    secondExaminer: String,
    examPhases: List[String],
    exams: List[ExamListModuleExam]
)

object ExamListModuleOption {
  given Format[ExamListAssessmentMethod] = nullable.format
  given Format[ExamListModuleExam]       = Json.format
  given Format[ExamListModuleOption]     = nullable.format
}

final case class ExamListPeriod(start: LocalDate, end: LocalDate)

/** Exam of an exam list entry with its name at publication. */
final case class ExamListExam(exam: UUID, name: String, registrationPeriods: Map[String, ExamListPeriod])

object ExamListExam {
  given Writes[ExamListPeriod] = Json.writes
  given Writes[ExamListExam]   = Json.writes
}

/** Module included in an exam list with its considered phases and exams, as rendered and stored on publication. */
final case class PreparedExamListEntry(
    module: ExamListModuleOption,
    examPhases: List[String],
    exams: List[ExamListExam]
)

/** Data read in one transaction to prepare the exam list of a PO. */
final case class ExamListData(
    studyProgram: StudyProgramView,
    options: Seq[ExamListModuleOption],
    specializations: Seq[Specialization],
    identities: Seq[Identity],
    assessmentMethods: Seq[AssessmentMethod],
    genericModules: Seq[ModuleCore]
)

final case class ExamList(id: UUID, studyProgram: StudyProgramView, semester: Semester, date: LocalDate, url: String)

object ExamList {
  given Writes[ExamList] = Json.writes
}

/** Stored module row of a published exam list. */
final case class ExamListEntry(
    id: UUID,
    examList: UUID,
    module: UUID,
    moduleTitle: String,
    moduleAbbrev: String,
    mandatory: Boolean,
    po: JsValue,
    assessmentMethods: List[String],
    firstExaminer: String,
    secondExaminer: String,
    examPhases: List[String],
    exams: JsValue
)

object ExamListEntry {
  given Writes[ExamListEntry] = Json.writes
}
