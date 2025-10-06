package printing.yaml

import java.util.UUID
import javax.inject.Singleton

import cats.data.NonEmptyList
import models.*
import parsing.metadata.*
import parsing.types.ModuleParticipants
import printer.Printer
import printer.Printer.always
import printer.Printer.newline
import printer.Printer.prefix
import printer.Printer.whitespace

// TODO replace all keys with those from Parser

@Singleton
final class MetadataYamlPrinter(identLevel: Int) {

  implicit val showUUID: UUID => String = _.toString

  implicit val showInt: Int => String = _.toString

  implicit def toCatsOrder[A](implicit ord: Ordering[A]): cats.Order[A] =
    cats.Order.fromOrdering(ord)

  implicit val ameOrd: Ordering[ModuleAssessmentMethodEntryProtocol] =
    Ordering.by[ModuleAssessmentMethodEntryProtocol, String](_.method)

  implicit val pomOrd: Ordering[ModulePOMandatoryProtocol] =
    Ordering.by[ModulePOMandatoryProtocol, String](_.po)

  implicit val poeOrd: Ordering[ModulePOOptionalProtocol] =
    Ordering.by[ModulePOOptionalProtocol, String](_.po)

  def printer(versionScheme: VersionScheme): Printer[(UUID, MetadataProtocol)] =
    Printer {
      case ((id, metadata), input) =>
        opener(versionScheme)
          .skip(this.id(id))
          .skip(title(metadata.title))
          .skip(abbreviation(metadata.abbrev))
          .skip(moduleType(metadata.moduleType))
          .skipOpt(metadata.moduleRelation.map(moduleRelation))
          .skip(ects(metadata.ects))
          .skip(language(metadata.language))
          .skip(duration(metadata.duration))
          .skip(frequency(metadata.season))
          .skip(
            responsibilities(
              metadata.moduleManagement,
              metadata.lecturers
            )
          )
          .skipOpt(
            NonEmptyList
              .fromList(metadata.assessmentMethods.mandatory)
              .map(assessmentMethodsMandatory)
          )
          .skip(examiner(metadata.examiner))
          .skip(examPhases(metadata.examPhases))
          .skip(workload(metadata.workload))
          .skipOpt(
            metadata.prerequisites.recommended.map(recommendedPrerequisites)
          )
          .skipOpt(
            metadata.prerequisites.required.map(requiredPrerequisites)
          )
          .skip(status(metadata.status))
          .skip(location(metadata.location))
          .skipOpt(NonEmptyList.fromList(metadata.po.mandatory).map(poMandatory))
          .skipOpt(NonEmptyList.fromList(metadata.po.optional).map(poOptional))
          .skipOpt(metadata.participants.map(participants))
          .skipOpt(NonEmptyList.fromList(metadata.taughtWith).map(taughtWith))
          .skipOpt(metadata.attendanceRequirement.map(attendanceRequirement))
          .skipOpt(metadata.assessmentPrerequisite.map(assessmentPrerequisite))
          .skip(closer())
          .print((), input)
    }

  private def entry(key: String, value: String) =
    prefix(s"$key: $value").skip(newline)

  def opener(versionScheme: VersionScheme) =
    prefix("---")
      .skip(this.versionScheme(versionScheme))
      .skip(newline)

  def closer() =
    prefix("---")

  def versionScheme(versionScheme: VersionScheme) =
    prefix(s"v${versionScheme.number}${versionScheme.label}")

  def moduleRelation(r: ModuleRelationProtocol) = {
    val relation = r match {
      case ModuleRelationProtocol.Parent(children) =>
        list(
          prefix("children:"),
          children,
          "module",
          identLevel
        )
      case ModuleRelationProtocol.Child(parent) =>
        entry("parent", s"module.$parent")
    }

    prefix("relation:")
      .skip(newline)
      .skip(whitespace.repeat(identLevel))
      .skip(relation)
  }

  def list[A](
      key: Printer[Unit],
      list: NonEmptyList[A],
      prefixLabel: String,
      identLevel: Int
  )(implicit toString: A => String, ord: Ordering[A]) = {
    def value(a: A) =
      if (prefixLabel.isEmpty) toString(a)
      else s"$prefixLabel.${toString(a)}"

    if (list.size == 1)
      key
        .skip(prefix(s" ${value(list.head)}"))
        .skip(newline)
    else
      key
        .skip(newline)
        .skip(
          list.sorted
            .map(id =>
              whitespace
                .repeat(identLevel + 2)
                .skip(prefix(s"- ${value(id)}"))
                .skip(newline)
            )
            .reduceLeft(_.skip(_))
        )
  }

  def examiner(e: Examiner.ID) =
    entry(ExaminerParser.firstKey, ExaminerParser.prefix + e.first)
      .skip(entry(ExaminerParser.secondKey, ExaminerParser.prefix + e.second))

  def examPhases(xs: NonEmptyList[String]) =
    list(
      prefix(s"${ExamPhaseParser.key}:"),
      xs,
      ExamPhaseParser.prefix.dropRight(1),
      0
    )

  def moduleType(moduleType: String) =
    entry(ModuleTypeParser.key, ModuleTypeParser.prefix + moduleType)

  def abbreviation(abbrev: String) =
    entry(THKV1Parser.abbreviationKey, abbrev)

  def title(title: String) =
    entry(THKV1Parser.titleKey, title)

  def id(value: UUID) =
    entry(THKV1Parser.idKey, value.toString)

  def ects(value: Double) =
    entry(ModuleECTSParser.key, value.toString)

  def language(value: String) =
    entry(ModuleLanguageParser.key, ModuleLanguageParser.prefix + value)

  def duration(value: Int) =
    entry(THKV1Parser.durationKey, value.toString)

  def frequency(value: String) =
    entry(ModuleSeasonParser.key, ModuleSeasonParser.prefix + value)

  def responsibilities(
      moduleManagement: NonEmptyList[String],
      lecturers: NonEmptyList[String]
  ) = {
    prefix(s"${ModuleResponsibilitiesParser.key}:")
      .skip(newline)
      .skip(whitespace.repeat(identLevel))
      .skip(
        list(
          prefix(s"${ModuleResponsibilitiesParser.moduleManagementKey}:"),
          moduleManagement,
          "person",
          identLevel
        )
      )
      .skip(whitespace.repeat(identLevel))
      .skip(
        list(
          prefix(s"${ModuleResponsibilitiesParser.lecturersKey}:"),
          lecturers,
          "person",
          identLevel
        )
      )
  }

  private def assessmentMethods(
      key: Printer[Unit],
      value: NonEmptyList[ModuleAssessmentMethodEntryProtocol]
  ) = {
    val deepness = identLevel + 2
    key
      .skip(newline)
      .skip(
        value.sorted
          .map(e => {
            whitespace
              .repeat(identLevel)
              .skip(
                prefix(
                  s"- method: ${ModuleAssessmentMethodParser.assessmentPrefix}${e.method}"
                )
              )
              .skip(newline)
              .skipOpt(
                e.percentage.map(d =>
                  whitespace
                    .repeat(deepness)
                    .skip(entry("percentage", d.toString))
                )
              )
              .skipOpt(
                NonEmptyList
                  .fromList(e.precondition)
                  .map(xs =>
                    whitespace
                      .repeat(deepness)
                      .skip(
                        list(
                          prefix(
                            s"${ModuleAssessmentMethodParser.preconditionKey}:"
                          ),
                          xs,
                          ModuleAssessmentMethodParser.assessmentPrefix
                            .dropRight(1),
                          deepness
                        )
                      )
                  )
              )
          })
          .reduceLeft(_.skip(_))
      )
  }

  def assessmentMethodsMandatory(
      value: NonEmptyList[ModuleAssessmentMethodEntryProtocol]
  ) =
    assessmentMethods(
      prefix(s"${ModuleAssessmentMethodParser.mandatoryKey}:"),
      value
    )

  def workload(workload: ModuleWorkload) =
    prefix(s"${ModuleWorkloadParser.key}:")
      .skip(newline)
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry(ModuleWorkloadParser.lectureKey, workload.lecture.toString)
          )
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry(ModuleWorkloadParser.seminarKey, workload.seminar.toString)
          )
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry(
              ModuleWorkloadParser.practicalKey,
              workload.practical.toString
            )
          )
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry(ModuleWorkloadParser.exerciseKey, workload.exercise.toString)
          )
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry(
              ModuleWorkloadParser.projectSupervisionKey,
              workload.projectSupervision.toString
            )
          )
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry(
              ModuleWorkloadParser.projectWorkKey,
              workload.projectWork.toString
            )
          )
      )

  private def prerequisites(
      key: Printer[Unit],
      value: ModulePrerequisiteEntryProtocol
  ) = {
    if (value.text.isEmpty && value.pos.isEmpty && value.modules.isEmpty)
      always[Unit]()
    else
      key
        .skip(newline)
        .skipOpt(
          Option.when(value.text.nonEmpty)(
            whitespace
              .repeat(identLevel)
              .skip(entry(ModulePrerequisitesParser.textKey, value.text))
          )
        )
        .skipOpt(
          NonEmptyList
            .fromList(value.modules)
            .map(xs =>
              whitespace
                .repeat(identLevel)
                .skip(
                  list(
                    prefix(s"${ModulePrerequisitesParser.modulesKey}:"),
                    xs,
                    ModulePrerequisitesParser.modulesPrefix.dropRight(1),
                    identLevel
                  )
                )
            )
        )
        .skipOpt(
          NonEmptyList
            .fromList(value.pos)
            .map(xs =>
              whitespace
                .repeat(identLevel)
                .skip(
                  list(
                    prefix(s"${ModulePrerequisitesParser.studyProgramsKey}:"),
                    xs,
                    ModulePrerequisitesParser.studyProgramsPrefix.dropRight(1),
                    identLevel
                  )
                )
            )
        )
  }

  def recommendedPrerequisites(value: ModulePrerequisiteEntryProtocol) =
    prerequisites(
      prefix(s"${ModulePrerequisitesParser.recommendedKey}:"),
      value
    )

  def requiredPrerequisites(value: ModulePrerequisiteEntryProtocol) =
    prerequisites(prefix(s"${ModulePrerequisitesParser.requiredKey}:"), value)

  def status(value: String) =
    entry(ModuleStatusParser.key, ModuleStatusParser.prefix + value)

  def location(value: String) =
    entry(ModuleLocationParser.key, ModuleLocationParser.prefix + value)

  def participants(value: ModuleParticipants) =
    prefix(s"${ModuleParticipantsParser.key}:")
      .skip(newline)
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(entry(ModuleParticipantsParser.minKey, value.min.toString))
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(entry(ModuleParticipantsParser.maxKey, value.max.toString))
      )

  def taughtWith(value: NonEmptyList[UUID]) =
    list(
      prefix(s"${ModuleTaughtWithParser.key}:"),
      value,
      ModuleTaughtWithParser.modulePrefix.dropRight(1),
      0
    )

  private def printPo(po: String, spec: Option[String]) = {
    val podId = spec match
      case Some(spec) => s"$po.$spec"
      case None       => po
    s"- ${ModulePOParser.studyProgramKey}: ${ModulePOParser.studyProgramPrefix}$podId"
  }

  def poMandatory(value: NonEmptyList[ModulePOMandatoryProtocol]) = {
    val deepness = identLevel + 2
    prefix(ModulePOParser.modulePOMandatoryKey)
      .skip(newline)
      .skip(
        value.sorted
          .map(e => {
            whitespace
              .repeat(identLevel)
              .skip(
                prefix(printPo(e.po, e.specialization))
              )
              .skip(newline)
              .skipOpt(
                NonEmptyList
                  .fromList(e.recommendedSemester)
                  .map(xs => recommendedSemester(xs, deepness))
              )
          })
          .reduceLeft(_.skip(_))
      )
  }

  def recommendedSemester(xs: NonEmptyList[Int], deepness: Int) =
    whitespace
      .repeat(deepness)
      .skip(
        list(
          prefix(s"${ModulePOParser.recommendedSemesterKey}:"),
          xs,
          "",
          deepness
        )
      )

  def poOptional(value: NonEmptyList[ModulePOOptionalProtocol]) = {
    val deepness = identLevel + 2
    prefix(ModulePOParser.modulePOElectiveKey)
      .skip(newline)
      .skip(
        value.sorted
          .map(e => {
            whitespace
              .repeat(identLevel)
              .skip(
                prefix(printPo(e.po, e.specialization))
              )
              .skip(newline)
              .skip(
                whitespace
                  .repeat(deepness)
                  .skip(
                    entry(
                      ModulePOParser.instanceOfKey,
                      s"${ModulePOParser.modulePrefix}${e.instanceOf}"
                    )
                  )
              )
              .skip(
                whitespace
                  .repeat(deepness)
                  .skip(
                    entry(
                      ModulePOParser.partOfCatalogKey,
                      e.partOfCatalog.toString
                    )
                  )
              )
              .skipOpt(
                NonEmptyList
                  .fromList(e.recommendedSemester)
                  .map(xs => recommendedSemester(xs, deepness))
              )
          })
          .reduceLeft(_.skip(_))
      )
  }

  def attendanceRequirement(attendanceRequirement: AttendanceRequirement) =
    prefix(s"${AttendanceRequirementParser.key}:")
      .skip(newline)
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry(AttendanceRequirementParser.minKey, attendanceRequirement.min)
          )
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry(AttendanceRequirementParser.reasonKey, attendanceRequirement.reason)
          )
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry(AttendanceRequirementParser.absenceKey, attendanceRequirement.absence)
          )
      )

  def assessmentPrerequisite(assessmentPrerequisite: AssessmentPrerequisite) =
    prefix(s"${AssessmentPrerequisiteParser.key}:")
      .skip(newline)
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry(AssessmentPrerequisiteParser.modulesKey, assessmentPrerequisite.modules)
          )
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry(AssessmentPrerequisiteParser.reasonKey, assessmentPrerequisite.reason)
          )
      )
}
