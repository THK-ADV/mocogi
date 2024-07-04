package printing.yaml

import cats.data.NonEmptyList
import models._
import parsing.metadata.VersionScheme
import parsing.types.ModuleParticipants
import printer.Printer
import printer.Printer.{always, newline, prefix, whitespace}

import java.util.UUID
import javax.inject.Singleton

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
    Printer { case ((id, metadata), input) =>
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
        .skipOpt(
          NonEmptyList
            .fromList(metadata.assessmentMethods.optional)
            .map(assessmentMethodsOptional)
        )
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
        .skipOpt(NonEmptyList.fromList(metadata.competences).map(competences))
        .skipOpt(
          NonEmptyList.fromList(metadata.globalCriteria).map(globalCriteria)
        )
        .skipOpt(NonEmptyList.fromList(metadata.taughtWith).map(taughtWith))
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
  )(implicit
      toString: A => String,
      ord: Ordering[A]
  ) = {
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

  def moduleType(moduleType: String) =
    entry("type", s"type.$moduleType")

  def abbreviation(abbrev: String) =
    entry("abbreviation", abbrev)

  def title(title: String) =
    entry("title", title)

  def id(value: UUID) =
    entry("id", value.toString)

  def ects(value: Double) =
    entry("ects", value.toString)

  def language(value: String) =
    entry("language", s"lang.$value")

  def duration(value: Int) =
    entry("duration", value.toString)

  def frequency(value: String) =
    entry("frequency", s"season.$value")

  def responsibilities(
      moduleManagement: NonEmptyList[String],
      lecturers: NonEmptyList[String]
  ) = {
    prefix("responsibilities:")
      .skip(newline)
      .skip(whitespace.repeat(identLevel))
      .skip(
        list(
          prefix("module_management:"),
          moduleManagement,
          "person",
          identLevel
        )
      )
      .skip(whitespace.repeat(identLevel))
      .skip(list(prefix("lecturers:"), lecturers, "person", identLevel))
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
              .skip(prefix(s"- method: assessment.${e.method}"))
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
                          prefix("precondition:"),
                          xs,
                          "assessment",
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
    assessmentMethods(prefix("assessment_methods_mandatory:"), value)

  def assessmentMethodsOptional(
      value: NonEmptyList[ModuleAssessmentMethodEntryProtocol]
  ) =
    assessmentMethods(prefix("assessment_methods_optional:"), value)

  def workload(workload: ModuleWorkload) =
    prefix("workload:")
      .skip(newline)
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(entry("lecture", workload.lecture.toString))
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(entry("seminar", workload.seminar.toString))
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(entry("practical", workload.practical.toString))
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(entry("exercise", workload.exercise.toString))
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(
            entry("project_supervision", workload.projectSupervision.toString)
          )
      )
      .skip(
        whitespace
          .repeat(identLevel)
          .skip(entry("project_work", workload.projectWork.toString))
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
            whitespace.repeat(identLevel).skip(entry("text", value.text))
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
                    prefix("modules:"),
                    xs,
                    "module",
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
                    prefix("study_programs:"),
                    xs,
                    "study_program",
                    identLevel
                  )
                )
            )
        )
  }

  def recommendedPrerequisites(value: ModulePrerequisiteEntryProtocol) =
    prerequisites(prefix("recommended_prerequisites:"), value)

  def requiredPrerequisites(value: ModulePrerequisiteEntryProtocol) =
    prerequisites(prefix("required_prerequisites:"), value)

  def status(value: String) =
    entry("status", s"status.$value")

  def location(value: String) =
    entry("location", s"location.$value")

  def participants(value: ModuleParticipants) =
    prefix("participants:")
      .skip(newline)
      .skip(
        whitespace.repeat(identLevel).skip(entry("min", value.min.toString))
      )
      .skip(
        whitespace.repeat(identLevel).skip(entry("max", value.max.toString))
      )

  def competences(value: NonEmptyList[String]) =
    list(prefix("competences:"), value, "competence", 0)

  def globalCriteria(value: NonEmptyList[String]) =
    list(prefix("global_criteria:"), value, "global_criteria", 0)

  def taughtWith(value: NonEmptyList[UUID]) =
    list(prefix("taught_with:"), value, "module", 0)

  def poMandatory(value: NonEmptyList[ModulePOMandatoryProtocol]) = {
    val deepness = identLevel + 2
    prefix("po_mandatory:")
      .skip(newline)
      .skip(
        value.sorted
          .map(e => {
            whitespace
              .repeat(identLevel)
              .skip(prefix(s"- study_program: study_program.${e.po}"))
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
          prefix("recommended_semester:"),
          xs,
          "",
          deepness
        )
      )

  def poOptional(value: NonEmptyList[ModulePOOptionalProtocol]) = {
    val deepness = identLevel + 2
    prefix("po_optional:")
      .skip(newline)
      .skip(
        value.sorted
          .map(e => {
            whitespace
              .repeat(identLevel)
              .skip(prefix(s"- study_program: study_program.${e.po}"))
              .skip(newline)
              .skip(
                whitespace
                  .repeat(deepness)
                  .skip(entry("instance_of", s"module.${e.instanceOf}"))
              )
              .skip(
                whitespace
                  .repeat(deepness)
                  .skip(entry("part_of_catalog", e.partOfCatalog.toString))
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
}
