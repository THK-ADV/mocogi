package validator

import parsing.types._
import validator.MetadataValidator.Validation

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

case class Validator[A, B](validate: A => Validation[B]) {
  def zip[C](that: Validator[A, C]): Validator[A, (B, C)] =
    Validator { a =>
      var maybeB = Option.empty[B]
      var maybeC = Option.empty[C]
      val errs = ListBuffer[String]()
      this.validate(a) match {
        case Right(b)  => maybeB = Some(b)
        case Left(err) => errs ++= err
      }
      that.validate(a) match {
        case Right(c)  => maybeC = Some(c)
        case Left(err) => errs ++= err
      }
      if (maybeC.isDefined && maybeB.isDefined)
        Right((maybeB.get, maybeC.get))
      else
        Left(errs.toList)
    }

  def map[C](f: B => C): Validator[A, C] =
    Validator(a => this.validate(a).map(f))

  def pullback[C](toLocalValue: C => A): Validator[C, B] =
    Validator { globalValue =>
      this.validate(toLocalValue(globalValue))
    }
}

case class SimpleValidator[A](validate: A => Validation[A]) {
  def pullback[B](toLocalValue: B => A): Validator[B, A] =
    Validator { globalValue =>
      this.validate(toLocalValue(globalValue))
    }
}

object MetadataValidator {

  type Validation[A] = Either[List[String], A]

  def assessmentMethodsValidator: SimpleValidator[AssessmentMethods] =
    SimpleValidator { am =>
      def sum(xs: List[AssessmentMethodEntry]): Double =
        xs.foldLeft(0.0) { case (acc, a) => acc + a.percentage.getOrElse(0.0) }

      def go(xs: List[AssessmentMethodEntry], name: String): List[String] = {
        val s = sum(xs)
        if (s == 0 || s == 100.0) Nil
        else List(s"$name sum must be null or 100, but was $s")
      }

      val res = go(am.mandatory, "mandatory") ++ go(am.optional, "optional")
      Either.cond(res.isEmpty, am, res)
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

  /*
  def workloadValidator: Validator = m => {
    if (!(m.workload.selfStudy == 0) || !(m.workload.total == 0))
      Left(List("workload: self study and total must be 0"))
    else if (m.credits.value == 0)
      Left(List("ects: value must be set"))
    else {
      def sumWorkload(w: Workload): Int =
        w.lecture +
          w.seminar +
          w.practical +
          w.exercise +
          w.projectSupervision +
          w.projectWork

      val total = (m.credits.value * 30).toInt // TODO
      val selfStudy = total - sumWorkload(m.workload)
      if (selfStudy < 0)
        Left(List("workload: self study must be positive"))
      else
        Right(
          m.copy(workload =
            m.workload.copy(selfStudy = selfStudy, total = total)
          )
        )
    }
  }*/

  def resolveModules[Module](
      modules: List[String],
      lookup: String => Option[Module]
  ): Either[List[String], List[Module]] = {
    val (errs, res) =
      modules.partitionMap(m => lookup(m).toRight(s"module not found: $m"))
    Either.cond(errs.isEmpty, res, errs)
  }

  def taughtWithValidator[Module](
      lookup: String => Option[Module]
  ): Validator[List[String], List[Module]] =
    Validator(modules => resolveModules(modules, lookup))

  def prerequisitesEntryValidator(
      lookup: String => Option[Module]
  ): Validator[Option[PrerequisiteEntry], Option[ValidPrerequisiteEntry]] =
    Validator {
      case Some(e) =>
        resolveModules(e.modules, lookup)
          .map(modules =>
            Some(ValidPrerequisiteEntry(e.text, modules, e.studyPrograms))
          )
      case None =>
        Right(None)
    }

  def prerequisitesValidator(
      lookup: String => Option[Module]
  ): Validator[Prerequisites, ValidPrerequisites] =
    prerequisitesEntryValidator(lookup)
      .pullback[Prerequisites](_.recommended)
      .zip(prerequisitesEntryValidator(lookup).pullback(_.required))
      .map(ValidPrerequisites.tupled)

  def assessmentMethodsValidatorAdapter
      : Validator[Metadata, AssessmentMethods] =
    assessmentMethodsValidator.pullback(_.assessmentMethods)

  def participantsValidatorAdapter: Validator[Metadata, Option[Participants]] =
    participantsValidator.pullback(_.participants)

  def ectsValidatorAdapter: Validator[Metadata, ECTS] =
    ectsValidator.pullback(_.credits)

  def prerequisitesValidatorAdapter(
      lookup: String => Option[Module]
  ): Validator[Metadata, ValidPrerequisites] =
    prerequisitesValidator(lookup).pullback(_.prerequisites)

  def taughtWithValidatorAdapter(
      lookup: String => Option[Module]
  ): Validator[Metadata, List[Module]] =
    taughtWithValidator(lookup).pullback(_.taughtWith)

  def validations(
      m: Metadata,
      lookup: String => Option[Module]
  ): Validator[Metadata, ValidMetadata] =
    assessmentMethodsValidatorAdapter
      .zip(participantsValidatorAdapter)
      .zip(ectsValidatorAdapter)
      .zip(taughtWithValidatorAdapter(lookup))
      .zip(prerequisitesValidatorAdapter(lookup))
      .map { case ((((a, b), c), d), e) =>
        ValidMetadata(m.id, a, b, c, d, e)
      }

  def validate(
      metadata: Seq[Metadata],
      lookup: String => Future[Metadata]
  ): Seq[Validation[ValidMetadata]] =
    metadata.map { m =>
      validations(m, _ => Option.empty[Module]).validate(m)
    }

}
