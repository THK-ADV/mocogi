package validator

import parsing.types._

import scala.collection.mutable.ListBuffer

object MetadataValidator {

  type Lookup = String => Option[Module]

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
  ): Validator[(Workload, ECTS), ValidWorkload] = {
    def sumWorkload(w: Workload): Int =
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
          ValidWorkload(
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

  def moduleValidator(lookup: Lookup): Validator[List[String], List[Module]] =
    Validator { modules =>
      val (errs, res) =
        modules.partitionMap(m => lookup(m).toRight(s"module not found: $m"))
      Either.cond(errs.isEmpty, res, errs)
    }

  def taughtWithValidator(
      lookup: Lookup
  ): Validator[List[String], List[Module]] =
    moduleValidator(lookup)

  def prerequisitesEntryValidator(
      lookup: Lookup
  ): Validator[Option[PrerequisiteEntry], Option[ValidPrerequisiteEntry]] =
    moduleValidator(lookup)
      .pullback[Option[PrerequisiteEntry]](_.map(_.modules).getOrElse(Nil))
      .map((p, ms) =>
        p.map(e => ValidPrerequisiteEntry(e.text, ms, e.studyPrograms))
      )

  def prerequisitesValidator(
      lookup: Lookup
  ): Validator[Prerequisites, ValidPrerequisites] =
    prerequisitesEntryValidator(lookup)
      .pullback[Prerequisites](_.recommended)
      .zip(prerequisitesEntryValidator(lookup).pullback(_.required))
      .map((_, p) => ValidPrerequisites.tupled(p))

  def poOptionalValidator(
      lookup: Lookup
  ): Validator[List[POOptional], List[ValidPOOptional]] =
    moduleValidator(lookup)
      .pullback[List[POOptional]](_.map(_.instanceOf))
      .map(_.zip(_).map { case (po, m) =>
        ValidPOOptional(
          po.studyProgram,
          m,
          po.partOfCatalog,
          po.recommendedSemester
        )
      })

  def posValidator(lookup: Lookup): Validator[POs, ValidPOs] =
    poOptionalValidator(lookup)
      .pullback[POs](_.optional)
      .map((pos, poOpt) => ValidPOs(pos.mandatory, poOpt))

  def moduleRelationValidator(
      lookup: Lookup
  ): Validator[Option[ModuleRelation], Option[ValidModuleRelation]] =
    moduleValidator(lookup)
      .pullback[Option[ModuleRelation]](_.map {
        case ModuleRelation.Parent(children) => children
        case ModuleRelation.Child(parent)    => List(parent)
      }.getOrElse(Nil))
      .map((r, ms) =>
        r.map {
          case ModuleRelation.Parent(_) => ValidModuleRelation.Parent(ms)
          case ModuleRelation.Child(_)  => ValidModuleRelation.Child(ms.head)
        }
      )

  def nonEmptyStringValidator(label: String): SimpleValidator[String] =
    SimpleValidator(s =>
      Either.cond(s.nonEmpty, s, List(s"$label must be set, but was empty"))
    )

  def titleValidatorAdapter(): Validator[Metadata, String] =
    nonEmptyStringValidator("title").pullback[Metadata](_.title)

  def abbrevValidatorAdapter(): Validator[Metadata, String] =
    nonEmptyStringValidator("abbrev").pullback[Metadata](_.abbrev)

  def assessmentMethodsValidatorAdapter
      : Validator[Metadata, AssessmentMethods] =
    assessmentMethodsValidator.pullback(_.assessmentMethods)

  def participantsValidatorAdapter: Validator[Metadata, Option[Participants]] =
    participantsValidator.pullback(_.participants)

  def ectsValidatorAdapter: Validator[Metadata, ECTS] =
    ectsValidator.pullback(_.credits)

  def prerequisitesValidatorAdapter(
      lookup: Lookup
  ): Validator[Metadata, ValidPrerequisites] =
    prerequisitesValidator(lookup).pullback(_.prerequisites)

  def taughtWithValidatorAdapter(
      lookup: Lookup
  ): Validator[Metadata, List[Module]] =
    taughtWithValidator(lookup).pullback(_.taughtWith)

  def workloadValidatorAdapter(
      creditPointFactor: Int,
      ects: ECTS
  ): Validator[Metadata, ValidWorkload] =
    workloadValidator(creditPointFactor).pullback(a => (a.workload, ects))

  def ectsWorkloadAdapter(
      creditPointFactor: Int
  ): Validator[Metadata, (ECTS, ValidWorkload)] =
    ectsValidatorAdapter
      .flatMap((_, ects) =>
        workloadValidatorAdapter(creditPointFactor, ects)
          .map((_, workload) => (ects, workload))
      )

  def posValidatorAdapter(lookup: Lookup): Validator[Metadata, ValidPOs] =
    posValidator(lookup).pullback(_.pos)

  def moduleRelationValidatorAdapter(
      lookup: Lookup
  ): Validator[Metadata, Option[ValidModuleRelation]] =
    moduleRelationValidator(lookup).pullback(_.relation)

  def validations(
      creditPointFactor: Int,
      lookup: Lookup
  ): Validator[Metadata, ValidMetadata] = {
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
          ValidMetadata(
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

  def toModule(m: Metadata): Module = Module(m.id, m.abbrev)

  def validate(
      metadata: Seq[Metadata],
      creditPointFactor: Int,
      lookup: String => Option[Metadata]
  ): Seq[Validation[ValidMetadata]] = {
    val validator = validations(creditPointFactor, lookup(_).map(toModule))
    metadata.map(m => validator.validate(m))
  }
}