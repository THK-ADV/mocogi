package validation

import java.util.UUID

import scala.collection.mutable.ListBuffer
import scala.util.Right

import cats.data.NonEmptyList
import models.*
import parsing.types.*

object MetadataValidator {

  private type Lookup = UUID => Option[ModuleCore]

  def assessmentMethodsValidator: SimpleValidator[ModuleAssessmentMethods] = {
    def sum(xs: List[ModuleAssessmentMethodEntry]): Double =
      xs.foldLeft(0.0) { case (acc, a) => acc + a.percentage.getOrElse(0.0) }

    SimpleValidator { am =>
      val s = sum(am.mandatory)
      Either.cond(s == 0 || s == 100.0, am, List(s"mandatory sum must be null or 100, but was $s"))
    }
  }

  def participantsValidator: SimpleValidator[Option[ModuleParticipants]] =
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

  def ectsValidator: Validator[Double, ModuleECTS] =
    Validator { ectsValue =>
      Either.cond(
        ectsValue != 0,
        ModuleECTS(ectsValue, Nil),
        List("ects value must be set if contributions to focus areas are empty")
      )
    }

  def workloadValidator: Validator[(ModuleWorkload, Double, Set[Int]), ModuleWorkload] =
    Validator {
      case (workload, _, ectsFactors) if ectsFactors.isEmpty =>
        Right(workload)
      case (workload, ects, ectsFactors) =>
        val ectsFactor = ectsFactors.min
        val total      = ModuleWorkload.totalHours(ects, ectsFactor)
        val selfStudy  = total - workload.sum()
        if (selfStudy < 0)
          Left(List(s"workload's self study must be positive to match ects $ects and ectsFactor $ectsFactor"))
        else Right(workload)
    }

  def moduleValidator(
      label: String,
      lookup: Lookup
  ): Validator[List[UUID], List[ModuleCore]] =
    Validator { modules =>
      val (errs, res) =
        modules.partitionMap(m => lookup(m).toRight(s"module in '$label' not found: $m"))
      Either.cond(errs.isEmpty, res, errs)
    }

  def taughtWithValidator(
      lookup: Lookup
  ): Validator[List[UUID], List[ModuleCore]] =
    moduleValidator("taught with", lookup)

  def prerequisitesEntryValidator(
      label: String,
      lookup: Lookup
  ): Validator[Option[ParsedPrerequisiteEntry], Option[
    ModulePrerequisiteEntry
  ]] =
    moduleValidator(label, lookup)
      .pullback[Option[ParsedPrerequisiteEntry]](
        _.map(_.modules).getOrElse(Nil)
      )
      .map((p, ms) => p.map(e => ModulePrerequisiteEntry(e.text, ms, e.studyPrograms)))

  def prerequisitesValidator(
      lookup: Lookup
  ): Validator[ParsedPrerequisites, ModulePrerequisites] =
    prerequisitesEntryValidator("recommended prerequisites", lookup)
      .pullback[ParsedPrerequisites](_.recommended)
      .zip(
        prerequisitesEntryValidator("required prerequisites", lookup)
          .pullback(_.required)
      )
      .map((_, p) => ModulePrerequisites.apply.tupled(p))

  def poOptionalValidator(
      lookup: Lookup
  ): Validator[List[ParsedPOOptional], List[ModulePOOptional]] =
    moduleValidator("po optional", lookup)
      .pullback[List[ParsedPOOptional]](_.map(_.instanceOf))
      .map(_.zip(_).map {
        case (po, m) =>
          ModulePOOptional(
            po.po,
            po.specialization,
            m,
            po.partOfCatalog,
            po.recommendedSemester
          )
      })

  def posValidator(lookup: Lookup): Validator[ParsedPOs, ModulePOs] =
    poOptionalValidator(lookup)
      .pullback[ParsedPOs](_.optional)
      .map((pos, poOpt) => models.ModulePOs(pos.mandatory, poOpt))

  def moduleRelationValidator(
      lookup: Lookup
  ): Validator[Option[ParsedModuleRelation], Option[ModuleRelation]] =
    moduleValidator("module relation", lookup)
      .pullback[Option[ParsedModuleRelation]] {
        case Some(ParsedModuleRelation.Parent(children)) => children.toList
        case Some(ParsedModuleRelation.Child(parent))    => List(parent)
        case None                                        => Nil
      }
      .map((r, ms) =>
        r.map {
          case ParsedModuleRelation.Parent(_) =>
            ModuleRelation.Parent(NonEmptyList.fromListUnsafe(ms))
          case ParsedModuleRelation.Child(_) =>
            ModuleRelation.Child(ms.head)
        }
      )

  def nonEmptyStringValidator(label: String): SimpleValidator[String] =
    SimpleValidator(s => Either.cond(s.nonEmpty, s, List(s"$label must be set, but was empty")))

  def titleValidatorAdapter(): Validator[ParsedMetadata, String] =
    nonEmptyStringValidator("title").pullback[ParsedMetadata](_.title)

  def abbrevValidatorAdapter(): Validator[ParsedMetadata, String] =
    nonEmptyStringValidator("abbrev").pullback[ParsedMetadata](_.abbrev)

  def assessmentMethodsValidatorAdapter: Validator[ParsedMetadata, ModuleAssessmentMethods] =
    assessmentMethodsValidator.pullback(_.assessmentMethods)

  def participantsValidatorAdapter: Validator[ParsedMetadata, Option[ModuleParticipants]] =
    participantsValidator.pullback(_.participants)

  def ectsValidatorAdapter: Validator[ParsedMetadata, ModuleECTS] =
    ectsValidator.pullback(_.credits)

  def prerequisitesValidatorAdapter(
      lookup: Lookup
  ): Validator[ParsedMetadata, ModulePrerequisites] =
    prerequisitesValidator(lookup).pullback(_.prerequisites)

  def taughtWithValidatorAdapter(
      lookup: Lookup
  ): Validator[ParsedMetadata, List[ModuleCore]] =
    taughtWithValidator(lookup).pullback(_.taughtWith)

  def workloadValidatorAdapter: Validator[ParsedMetadata, ModuleWorkload] =
    workloadValidator.pullback(a =>
      (a.workload, a.credits, (a.pos.mandatory.map(_.po.ectsFactor) ::: a.pos.optional.map(_.po.ectsFactor)).toSet)
    )

  def posValidatorAdapter(
      lookup: Lookup
  ): Validator[ParsedMetadata, ModulePOs] =
    posValidator(lookup).pullback(_.pos)

  def moduleRelationValidatorAdapter(
      lookup: Lookup
  ): Validator[ParsedMetadata, Option[ModuleRelation]] =
    moduleRelationValidator(lookup).pullback(_.relation)

  def validations(lookup: Lookup): Validator[ParsedMetadata, Metadata] = {
    titleValidatorAdapter()
      .zip(abbrevValidatorAdapter())
      .zip(assessmentMethodsValidatorAdapter)
      .zip(participantsValidatorAdapter)
      .zip(ectsValidatorAdapter)
      .zip(workloadValidatorAdapter)
      .zip(taughtWithValidatorAdapter(lookup))
      .zip(prerequisitesValidatorAdapter(lookup))
      .zip(posValidatorAdapter(lookup))
      .zip(moduleRelationValidatorAdapter(lookup))
      .map {
        case (
              m,
              (((((((((t, abbrev), am), part), ects), wl), tw), pre), pos), rel)
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
            m.examiner,
            m.examPhases,
            wl,
            pre,
            m.status,
            m.location,
            pos,
            part,
            m.competences,
            m.globalCriteria,
            tw,
            m.attendanceRequirement,
            m.assessmentPrerequisite
          )
      }
  }

  def validateMany(metadata: Seq[ParsedMetadata], lookup: Lookup): Seq[Validation[Metadata]] = {
    val validator = validations(lookup)
    metadata.map(m => validator.validate(m))
  }

  def validate(lookup: Lookup)(metadata: ParsedMetadata): Validation[Metadata] = {
    val validator = validations(lookup)
    validator.validate(metadata)
  }
}
