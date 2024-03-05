package printing.yaml

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
        .skip(
          assessmentMethodsMandatory(
            metadata.assessmentMethods.mandatory
          )
        )
        .skipOpt(
          opt(metadata.assessmentMethods.optional).map(
            assessmentMethodsOptional
          )
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
        .skip(poMandatory(metadata.po.mandatory))
        .skipOpt(opt(metadata.po.optional).map(poOptional))
        .skipOpt(metadata.participants.map(participants))
        .skipOpt(opt(metadata.competences).map(competences))
        .skipOpt(opt(metadata.globalCriteria).map(globalCriteria))
        .skipOpt(opt(metadata.taughtWith).map(taughtWith))
        .skip(closer())
        .print((), input)
    }

  private def opt[A](xs: List[A]): Option[List[A]] =
    Option.when(xs.nonEmpty)(xs)

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
        list(prefix("children:"), children, "module", identLevel)
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
      list: List[A],
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
            .reduce(_.skip(_))
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
      moduleManagement: List[String],
      lecturers: List[String]
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
      value: List[ModuleAssessmentMethodEntryProtocol]
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
                Option.when(e.precondition.nonEmpty)(
                  whitespace
                    .repeat(deepness)
                    .skip(
                      list(
                        prefix("precondition:"),
                        e.precondition.sorted,
                        "assessment",
                        deepness
                      )
                    )
                )
              )
          })
          .reduce(_.skip(_))
      )
  }

  def assessmentMethodsMandatory(
      value: List[ModuleAssessmentMethodEntryProtocol]
  ) =
    assessmentMethods(prefix("assessment_methods_mandatory:"), value)

  def assessmentMethodsOptional(
      value: List[ModuleAssessmentMethodEntryProtocol]
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
          Option.when(value.modules.nonEmpty)(
            whitespace
              .repeat(identLevel)
              .skip(
                list(
                  prefix("modules:"),
                  value.modules,
                  "module",
                  identLevel
                )
              )
          )
        )
        .skipOpt(
          Option.when(value.pos.nonEmpty)(
            whitespace
              .repeat(identLevel)
              .skip(
                list(
                  prefix("study_programs:"),
                  value.pos,
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

  def competences(value: List[String]) =
    list(prefix("competences:"), value, "competence", 0)

  def globalCriteria(value: List[String]) =
    list(prefix("global_criteria:"), value, "global_criteria", 0)

  def taughtWith(value: List[UUID]) =
    list(prefix("taught_with:"), value, "module", 0)

  def poMandatory(value: List[ModulePOMandatoryProtocol]) = {
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
              .skip(
                whitespace
                  .repeat(deepness)
                  .skip(
                    list(
                      prefix("recommended_semester:"),
                      e.recommendedSemester,
                      "",
                      deepness
                    )
                  )
              )
          })
          .reduce(_.skip(_))
      )
  }

  def poOptional(value: List[ModulePOOptionalProtocol]) = {
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
              .skipOpt(
                e.instanceOf.map(id =>
                  whitespace
                    .repeat(deepness)
                    .skip(entry("instance_of", s"module.$id"))
                )
              )
              .skip(
                whitespace
                  .repeat(deepness)
                  .skip(entry("focus", e.isFocus.toString))
              )
              .skip(
                whitespace
                  .repeat(deepness)
                  .skip(
                    list(
                      prefix("recommended_semester:"),
                      e.recommendedSemester,
                      "",
                      deepness
                    )
                  )
              )
          })
          .reduce(_.skip(_))
      )
  }
}
