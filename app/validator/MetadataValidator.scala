package validator

import models.Module
import parsing.types._

import java.util.UUID
import scala.collection.mutable.ListBuffer

object MetadataValidator {

  private type Lookup = UUID => Option[Module]

  def assessmentMethodsValidator: SimpleValidator[AssessmentMethods] = {
    def sum(xs: List[AssessmentMethodEntry]): Double =
      xs.foldLeft(0.0) { case (acc, a) => acc + a.percentage.getOrElse(0.0) }

    def go(xs: List[AssessmentMethodEntry], name: String): List[String] = {
      val s = sum(xs)
      if (s == 0 || s == 100.0) Nil
      else List(s"$name sum must be null or 100, but was $s")
    }

    SimpleValidator { am =>
      val res = go(am.mandatory, "mandatory") ++ go(am.optional, "optional")
      Either.cond(res.isEmpty, am, res)
    }
  }

  def participantsValidator: SimpleValidator[Option[Participants]] =
    SimpleValidator {
      case Some(p) =>
        val errs = ListBuffer[String]()
        if (p.min < 0)
          errs += s"participants min must be positive, but was ${p.min}"
        if (p.max < 0)
          errs += s"participants max must be positive, but was ${p.max}"
        if (!(p.min < p.max))
          errs += s"participants min must be lower than max. min: ${p.min}, max: ${p.max}"
        Either.cond(errs.isEmpty, Some(p), errs.toList)
      case None => Right(None)
    }

  def ectsValidator
      : Validator[Either[Double, List[ECTSFocusAreaContribution]], ECTS] =
    Validator {
      case Left(ectsValue) =>
        Either.cond(
          ectsValue != 0,
          ECTS(ectsValue, Nil),
          List(
            "ects value must be set if contributions to focus areas are empty"
          )
        )
      case Right(contributions) =>
        Either.cond(
          contributions.nonEmpty, {
            val ectsValue = contributions.foldLeft(0.0) { case (acc, a) =>
              acc + a.ectsValue
            }
            ECTS(ectsValue, contributions)
          },
          List(
            "ects contributions to focus areas must be set if ects value is 0"
          )
        )
    }

  def workloadValidator(
      creditPointFactor: Int
  ): Validator[(ParsedWorkload, ECTS), Workload] = {
    def sumWorkload(w: ParsedWorkload): Int =
      w.lecture +
        w.seminar +
        w.practical +
        w.exercise +
        w.projectSupervision +
        w.projectWork

    Validator { case (workload, ects) =>
      val total = (ects.value * creditPointFactor).toInt
      val selfStudy = total - sumWorkload(workload)
      if (selfStudy < 0)
        Left(List("workload's self study must be positive"))
      else
        Right(
          Workload(
            workload.lecture,
            workload.seminar,
            workload.practical,
            workload.exercise,
            workload.projectSupervision,
            workload.projectWork,
            selfStudy,
            total
          )
        )
    }
  }

  def moduleValidator(
      label: String,
      lookup: Lookup
  ): Validator[List[UUID], List[Module]] =
    Validator { modules =>
      val (errs, res) =
        modules.partitionMap(m =>
          lookup(m).toRight(s"module in '$label' not found: $m")
        )
      Either.cond(errs.isEmpty, res, errs)
    }

  def taughtWithValidator(
      lookup: Lookup
  ): Validator[List[UUID], List[Module]] =
    moduleValidator("taught with", lookup)

  def prerequisitesEntryValidator(
      label: String,
      lookup: Lookup
  ): Validator[Option[ParsedPrerequisiteEntry], Option[PrerequisiteEntry]] =
    moduleValidator(label, lookup)
      .pullback[Option[ParsedPrerequisiteEntry]](
        _.map(_.modules).getOrElse(Nil)
      )
      .map((p, ms) =>
        p.map(e => PrerequisiteEntry(e.text, ms, e.studyPrograms))
      )

  def prerequisitesValidator(
      lookup: Lookup
  ): Validator[ParsedPrerequisites, Prerequisites] =
    prerequisitesEntryValidator("recommended prerequisites", lookup)
      .pullback[ParsedPrerequisites](_.recommended)
      .zip(
        prerequisitesEntryValidator("required prerequisites", lookup)
          .pullback(_.required)
      )
      .map((_, p) => (Prerequisites.apply _).tupled(p))

  def poOptionalValidator(
      lookup: Lookup
  ): Validator[List[ParsedPOOptional], List[POOptional]] =
    moduleValidator("po optional", lookup)
      .pullback[List[ParsedPOOptional]](_.map(_.instanceOf))
      .map(_.zip(_).map { case (po, m) =>
        POOptional(
          po.po,
          po.specialization,
          m,
          po.partOfCatalog,
          po.recommendedSemester
        )
      })

  def posValidator(lookup: Lookup): Validator[ParsedPOs, POs] =
    poOptionalValidator(lookup)
      .pullback[ParsedPOs](_.optional)
      .map((pos, poOpt) => POs(pos.mandatory, poOpt))

  def moduleRelationValidator(
      lookup: Lookup
  ): Validator[Option[ParsedModuleRelation], Option[ModuleRelation]] =
    moduleValidator("module relation", lookup)
      .pullback[Option[ParsedModuleRelation]](_.map {
        case ParsedModuleRelation.Parent(children) => children
        case ParsedModuleRelation.Child(parent)    => List(parent)
      }.getOrElse(Nil))
      .map((r, ms) =>
        r.map {
          case ParsedModuleRelation.Parent(_) => ModuleRelation.Parent(ms)
          case ParsedModuleRelation.Child(_)  => ModuleRelation.Child(ms.head)
        }
      )

  def nonEmptyStringValidator(label: String): SimpleValidator[String] =
    SimpleValidator(s =>
      Either.cond(s.nonEmpty, s, List(s"$label must be set, but was empty"))
    )

  def titleValidatorAdapter(): Validator[ParsedMetadata, String] =
    nonEmptyStringValidator("title").pullback[ParsedMetadata](_.title)

  def abbrevValidatorAdapter(): Validator[ParsedMetadata, String] =
    nonEmptyStringValidator("abbrev").pullback[ParsedMetadata](_.abbrev)

  def assessmentMethodsValidatorAdapter
      : Validator[ParsedMetadata, AssessmentMethods] =
    assessmentMethodsValidator.pullback(_.assessmentMethods)

  def participantsValidatorAdapter
      : Validator[ParsedMetadata, Option[Participants]] =
    participantsValidator.pullback(_.participants)

  def ectsValidatorAdapter: Validator[ParsedMetadata, ECTS] =
    ectsValidator.pullback(_.credits)

  def prerequisitesValidatorAdapter(
      lookup: Lookup
  ): Validator[ParsedMetadata, Prerequisites] =
    prerequisitesValidator(lookup).pullback(_.prerequisites)

  def taughtWithValidatorAdapter(
      lookup: Lookup
  ): Validator[ParsedMetadata, List[Module]] =
    taughtWithValidator(lookup).pullback(_.taughtWith)

  def workloadValidatorAdapter(
      creditPointFactor: Int,
      ects: ECTS
  ): Validator[ParsedMetadata, Workload] =
    workloadValidator(creditPointFactor).pullback(a => (a.workload, ects))

  def ectsWorkloadAdapter(
      creditPointFactor: Int
  ): Validator[ParsedMetadata, (ECTS, Workload)] =
    ectsValidatorAdapter
      .flatMap((_, ects) =>
        workloadValidatorAdapter(creditPointFactor, ects)
          .map((_, workload) => (ects, workload))
      )

  def posValidatorAdapter(lookup: Lookup): Validator[ParsedMetadata, POs] =
    posValidator(lookup).pullback(_.pos)

  def moduleRelationValidatorAdapter(
      lookup: Lookup
  ): Validator[ParsedMetadata, Option[ModuleRelation]] =
    moduleRelationValidator(lookup).pullback(_.relation)

  def validations(
      creditPointFactor: Int,
      lookup: Lookup
  ): Validator[ParsedMetadata, Metadata] = {
    titleValidatorAdapter()
      .zip(abbrevValidatorAdapter())
      .zip(assessmentMethodsValidatorAdapter)
      .zip(participantsValidatorAdapter)
      .zip(ectsWorkloadAdapter(creditPointFactor))
      .zip(taughtWithValidatorAdapter(lookup))
      .zip(prerequisitesValidatorAdapter(lookup))
      .zip(posValidatorAdapter(lookup))
      .zip(moduleRelationValidatorAdapter(lookup))
      .map {
        case (
              m,
              ((((((((t, abbrev), am), part), (ects, wl)), tw), pre), pos), rel)
            ) =>
          Metadata(
            m.id,
            t,
            abbrev,
            m.kind,
            rel,
            ects,
            m.language,
            m.duration,
            m.season,
            m.responsibilities,
            am,
            wl,
            pre,
            m.status,
            m.location,
            pos,
            part,
            m.competences,
            m.globalCriteria,
            tw
          )
      }
  }

  def validateMany(
      metadata: Seq[ParsedMetadata],
      creditPointFactor: Int,
      lookup: Lookup
  ): Seq[Validation[Metadata]] = {
    val validator = validations(creditPointFactor, lookup)
    metadata.map(m => validator.validate(m))
  }

  def validate(creditPointFactor: Int, lookup: Lookup)(
      metadata: ParsedMetadata
  ): Validation[Metadata] = {
    val validator = validations(creditPointFactor, lookup)
    validator.validate(metadata)
  }
}
