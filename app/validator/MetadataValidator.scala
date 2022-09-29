package validator

import parsing.types._
import validator.MetadataValidator.Validation

import java.util.UUID
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
}

case class SimpleValidator[A](validate: A => Validation[A])

object MetadataValidator {

  type Validation[A] = Either[List[String], A]
  type Module = (UUID, String)

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
      modules.partitionMap(m => lookup(m).toRight(s"missing module: $m"))
    Either.cond(errs.isEmpty, res, errs)
  }

  def taughtWithValidator[Module](
      lookup: String => Option[Module]
  ): Validator[List[String], List[Module]] =
    Validator(modules => resolveModules(modules, lookup))

  def prerequisitesValidator[Module](
      lookup: String => Option[Module]
  ): Validator[Prerequisites, (List[Module], List[Module])] =
    Validator { prerequisites =>
      prerequisites.required.map(r => resolveModules(r.modules, lookup))
      ???
    }

  def simplePullback[GlobalValue, LocalValue](
      validator: SimpleValidator[LocalValue],
      toLocalValue: GlobalValue => LocalValue
  ): Validator[GlobalValue, LocalValue] =
    Validator { globalValue =>
      validator.validate(toLocalValue(globalValue))
    }

  def pullback[GlobalValue, LocalValue, NewLocalValue](
      validator: Validator[LocalValue, NewLocalValue],
      toLocalValue: GlobalValue => LocalValue
  ): Validator[GlobalValue, NewLocalValue] =
    Validator { globalValue =>
      validator.validate(toLocalValue(globalValue))
    }

  def assessmentMethodsValidatorAdapter
      : Validator[Metadata, AssessmentMethods] =
    simplePullback(assessmentMethodsValidator, _.assessmentMethods)

  def participantsValidatorAdapter: Validator[Metadata, Option[Participants]] =
    simplePullback(participantsValidator, _.participants)

  def ectsValidatorAdapter: Validator[Metadata, ECTS] =
    pullback(ectsValidator, _.credits)

  def taughtWithValidatorAdapter(
      lookup: String => Option[Module]
  ): Validator[Metadata, List[Module]] =
    pullback(taughtWithValidator(lookup), _.taughtWith)

  def validations(
      m: Metadata,
      lookup: String => Option[Module]
  ): Validator[Metadata, ValidMetadata] =
    assessmentMethodsValidatorAdapter
      .zip(participantsValidatorAdapter)
      .zip(ectsValidatorAdapter)
      .zip(taughtWithValidatorAdapter(lookup))
      .map { case (((a, b), c), d) =>
        ValidMetadata(m.id, a, b, c, d)
      }

  def validate(
      metadata: Seq[Metadata],
      lookup: String => Future[Metadata]
  ): Seq[Validation[ValidMetadata]] =
    metadata.map { m =>
      validations(m, _ => Option.empty[Module]).validate(m)
    }

}
