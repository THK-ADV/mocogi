package validator

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import parsing.types._
import validator.MetadataValidator._

final class MetadataValidatorSpec
    extends AnyWordSpec
    with EitherValues
    with OptionValues {

  private lazy val am = AssessmentMethod("", "", "")
  private lazy val fa = FocusArea("")

  private def method(percentage: Option[Double]) =
    AssessmentMethodEntry(am, percentage, Nil)

  private def ectsContrib(value: Double) =
    ECTSFocusAreaContribution(fa, value, "")

  "A Metadata Validator" when {
    "validating assessment methods" should {
      "pass if their percentage is 0" in {
        val am1 = AssessmentMethods(Nil, Nil)
        assert(assessmentMethodsValidator.validate(am1).value == am1)

        val am2 = AssessmentMethods(List(method(None)), Nil)
        assert(assessmentMethodsValidator.validate(am2).value == am2)

        val am3 = AssessmentMethods(List(method(None)), List(method(None)))
        assert(assessmentMethodsValidator.validate(am3).value == am3)

        val am4 =
          AssessmentMethods(List(method(Some(0))), List(method(Some(0))))
        assert(assessmentMethodsValidator.validate(am4).value == am4)
      }

      "pass if their percentage matches 100" in {
        val am1 =
          AssessmentMethods(List(method(Some(30)), method(Some(70))), Nil)
        assert(assessmentMethodsValidator.validate(am1).value == am1)

        val am2 = AssessmentMethods(List(method(Some(100))), Nil)
        assert(assessmentMethodsValidator.validate(am2).value == am2)

        val am3 = AssessmentMethods(Nil, List(method(Some(100))))
        assert(assessmentMethodsValidator.validate(am3).value == am3)

        val am4 = AssessmentMethods(
          List(method(Some(50)), method(Some(50))),
          List(method(Some(100)))
        )
        assert(assessmentMethodsValidator.validate(am4).value == am4)
      }

      "fail if their percentage doesn't match 100" in {
        val am1 =
          AssessmentMethods(List(method(Some(10)), method(Some(10))), Nil)
        assert(
          assessmentMethodsValidator.validate(am1).left.value == List(
            "mandatory sum must be null or 100, but was 20.0"
          )
        )

        val am2 = AssessmentMethods(List(method(Some(50))), Nil)
        assert(
          assessmentMethodsValidator.validate(am2).left.value == List(
            "mandatory sum must be null or 100, but was 50.0"
          )
        )

        val am3 = AssessmentMethods(Nil, List(method(Some(30))))
        assert(
          assessmentMethodsValidator.validate(am3).left.value == List(
            "optional sum must be null or 100, but was 30.0"
          )
        )

        val am4 = AssessmentMethods(
          List(method(Some(20)), method(Some(20))),
          List(method(Some(100)))
        )
        assert(
          assessmentMethodsValidator.validate(am4).left.value == List(
            "mandatory sum must be null or 100, but was 40.0"
          )
        )

        val am5 = AssessmentMethods(
          List(method(Some(20)), method(Some(20))),
          List(method(Some(50)))
        )
        assert(
          assessmentMethodsValidator.validate(am5).left.value == List(
            "mandatory sum must be null or 100, but was 40.0",
            "optional sum must be null or 100, but was 50.0"
          )
        )
      }
    }

    "validating participants" should {
      "pass if participants are not set" in {
        assert(participantsValidator.validate(None).value.isEmpty)
      }

      "pass if participants minimum is higher than 0" in {
        val p1 = Participants(0, 10)
        assert(
          participantsValidator.validate(Some(p1)).value.value == p1
        )
      }

      "fail if participants minimum or maximum is lower than 0" in {
        val p1 = Participants(-1, 10)
        assert(
          participantsValidator.validate(Some(p1)).left.value == List(
            "participants min must be positive, but was -1"
          )
        )

        val p2 = Participants(0, -1)
        assert(
          participantsValidator.validate(Some(p2)).left.value == List(
            "participants max must be positive, but was -1",
            "participants min must be lower than max. min: 0, max: -1"
          )
        )
      }

      "fail if participants minimum is higher than max" in {
        val p1 = Participants(10, 10)
        assert(
          participantsValidator.validate(Some(p1)).left.value == List(
            "participants min must be lower than max. min: 10, max: 10"
          )
        )

        val p2 = Participants(11, 10)
        assert(
          participantsValidator.validate(Some(p2)).left.value == List(
            "participants min must be lower than max. min: 11, max: 10"
          )
        )
      }

      "pass if participants ranges are valid" in {
        val p1 = Participants(5, 10)
        assert(
          participantsValidator.validate(Some(p1)).value.value == p1
        )
      }
    }

    "validating ects" should {
      "pass if ects value is set via contributions to focus areas" in {
        val ects1 = List(ectsContrib(5))
        assert(ectsValidator.validate(Right(ects1)).value == ECTS(5, ects1))
        val ects2 = List(ectsContrib(5), ectsContrib(3))
        assert(ectsValidator.validate(Right(ects2)).value == ECTS(8, ects2))
      }

      "pass if ects value is already set" in {
        assert(ectsValidator.validate(Left(5)).value == ECTS(5, Nil))
      }

      "fail if neither ects value nor contributions to focus areas are set" in {
        assert(
          ectsValidator.validate(Left(0)).left.value == List(
            "ects value must be set if contributions to focus areas are empty"
          )
        )
      }

      "fail if ects value is already set, but there are also contributions to focus areas" in {
        assert(
          ectsValidator.validate(Right(Nil)).left.value == List(
            "ects contributions to focus areas must be set if ects value is 0"
          )
        )
      }
    }

    "validating taught with" should {
      "pass if all modules are found" in {
        val modules = List[String]("m1", "m2", "m3")

        def lookup(module: String): Option[String] =
          modules.find(_ == module)

        assert(
          taughtWithValidator(lookup).validate(List("m1", "m2")).value == List(
            "m1",
            "m2"
          )
        )

        assert(
          taughtWithValidator(lookup).validate(List("m1")).value == List("m1")
        )

        assert(
          taughtWithValidator(lookup).validate(Nil).value.isEmpty
        )
      }

      "fail if one module can't be found" in {
        val modules = List[String]("m1", "m2", "m3")

        def lookup(module: String): Option[String] =
          modules.find(_ == module)

        assert(
          taughtWithValidator(lookup)
            .validate(List("m1", "m4", "m5"))
            .left
            .value == List("missing module: m4", "missing module: m5")
        )
      }
    }
  }
}
