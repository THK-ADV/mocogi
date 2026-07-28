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
        ModuleECTS(ectsValue),
        List("ects value must be set")
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
      .map((p, ms) => p.map(e => ModulePrerequisiteEntry(e.text, ms)))

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
      moduleId: UUID,
      lookup: Lookup,
      graph: ModuleRelationGraph
  ): Validator[Option[NonEmptyList[UUID]], Option[ModuleRelation]] =
    moduleRelationGraphValidator(moduleId, graph)
      .pullback[Option[NonEmptyList[UUID]]](identity)
      .zip(
        moduleValidator("module relation", lookup)
          .pullback[Option[NonEmptyList[UUID]]](_.toList.flatMap(_.toList))
      )
      .map((_, validated) => validated._1.map(_ => ModuleRelation(NonEmptyList.fromListUnsafe(validated._2))))

  /**
   * Ensures that the relation graph stays a forest of depth one: no module is both a parent and a
   * child, and every child has exactly one parent.
   */
  def moduleRelationGraphValidator(
      moduleId: UUID,
      graph: ModuleRelationGraph
  ): SimpleValidator[Option[NonEmptyList[UUID]]] =
    SimpleValidator { relation =>
      val children = relation.toList.flatMap(_.toList)
      val resolved = graph.updated(moduleId, relation)
      val errors   = ListBuffer.empty[String]

      if children.contains(moduleId) then errors += s"module relation must not reference itself: $moduleId"

      val duplicates = children.diff(children.distinct).distinct.sortBy(_.toString)
      if duplicates.nonEmpty then errors += s"module relation contains duplicate children: ${duplicates.mkString(", ")}"

      children.distinct.sortBy(_.toString).foreach { child =>
        val parents = resolved.parentsOf(child)
        if parents.size > 1 then
          errors += s"module relation child $child has multiple parents: ${parents.toList.sortBy(_.toString).mkString(", ")}"
        if child != moduleId && resolved.isParent(child) then
          errors += s"modules cannot be both parent and child: $child"
      }

      if children.nonEmpty && resolved.parentsOf(moduleId).exists(_ != moduleId) then
        errors += s"modules cannot be both parent and child: $moduleId"

      Either.cond(errors.isEmpty, relation, errors.toList)
    }

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
      lookup: Lookup,
      graph: ModuleRelationGraph
  ): Validator[ParsedMetadata, Option[ModuleRelation]] =
    Validator(metadata => moduleRelationValidator(metadata.id, lookup, graph).validate(metadata.relation))

  def validations(
      lookup: Lookup,
      graph: ModuleRelationGraph
  ): Validator[ParsedMetadata, Metadata] = {
    titleValidatorAdapter()
      .zip(abbrevValidatorAdapter())
      .zip(assessmentMethodsValidatorAdapter)
      .zip(participantsValidatorAdapter)
      .zip(ectsValidatorAdapter)
      .zip(workloadValidatorAdapter)
      .zip(taughtWithValidatorAdapter(lookup))
      .zip(prerequisitesValidatorAdapter(lookup))
      .zip(posValidatorAdapter(lookup))
      .zip(moduleRelationValidatorAdapter(lookup, graph))
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
            tw,
            m.attendanceRequirement,
            m.assessmentPrerequisite
          )
      }
  }

  /**
   * Validates each module against the relation graph that results from applying all of their
   * relations, so that modules of the same batch cannot claim the same child.
   */
  def validateMany(
      metadata: Seq[ParsedMetadata],
      lookup: Lookup,
      graph: ModuleRelationGraph
  ): Seq[Validation[Metadata]] = {
    val resolved  = metadata.foldLeft(graph)((acc, module) => acc.updated(module.id, module.relation))
    val validator = validations(lookup, resolved)
    metadata.map(m => validator.validate(m))
  }

  def validate(
      lookup: Lookup,
      graph: ModuleRelationGraph
  )(metadata: ParsedMetadata): Validation[Metadata] =
    validations(lookup, graph).validate(metadata)
}
