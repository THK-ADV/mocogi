package validator

import parsing.types.{AssessmentMethodEntry, Metadata, Workload}

import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

object MetadataValidator {

  type Validation[A] = Either[List[String], A]
  type Module = (UUID, String, String, String) // id, abbrev, title, gitPath

  type Validator = Metadata => Validation[Metadata]

  def assessmentMethodsValidator: Validator = m => {
    def sum(xs: List[AssessmentMethodEntry]): Double =
      xs.foldLeft(0.0) { case (acc, a) => acc + a.percentage.getOrElse(0.0) }

    val mandatorySum = sum(m.assessmentMethods.mandatory)
    val optionalSum = sum(m.assessmentMethods.optional)
    if (mandatorySum > 0 && mandatorySum == 100.0)
      if (optionalSum > 0 && optionalSum == 100.0) Right(m)
      else Left(List(s"optional sum must be 100, but was $optionalSum"))
    else Left(List(s"mandatory sum must be 100, but was $mandatorySum"))
  }

  def participantsValidator: Validator = m => {
    m.participants match {
      case Some(p) =>
        if (p.min >= 0 && p.min < p.max) Right(m)
        else
          Left(
            List(
              s"participants min must be positive and lower than max. min: ${p.min}, max: ${p.max}"
            )
          )
      case None => Right(m)
    }
  }

  def ectsValidator: Validator = m => {
    if (m.credits.contributionsToFocusAreas.isEmpty || !(m.credits.value == 0))
      Left(
        List(
          "ects: contributions to focus areas are set, but ects value is not 0"
        )
      )
    else {
      val ectsValue = m.credits.contributionsToFocusAreas.foldLeft(0.0) {
        case (acc, a) => acc + a.ectsValue
      }
      Right(m.copy(credits = m.credits.copy(ectsValue)))
    }
  }

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
  }

  def combine(validators: Validator*): Validator = m => {
    val it = validators.iterator
    val errs = ListBuffer[String]()
    while (it.hasNext) {
      it.next().apply(m) match {
        case Right(_) =>
        case Left(e)  => errs ++= e
      }
    }
    Either.cond(errs.isEmpty, m, errs.toList)
  }

  def validate(
      metadata: Seq[Metadata]
  )(lookup: String => Future[Metadata]): Seq[Validation[Metadata]] = {
    val validations = combine(
      assessmentMethodsValidator,
      participantsValidator,
      ectsValidator,
      workloadValidator
    )
    metadata.map(validations)
  }

}
