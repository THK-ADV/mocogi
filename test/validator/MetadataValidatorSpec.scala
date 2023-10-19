package validator

import models.Module
import models.core._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import parsing.types._
import validator.MetadataValidator._

import java.time.LocalDate
import java.util.UUID

final class MetadataValidatorSpec
    extends AnyWordSpec
    with EitherValues
    with OptionValues {

  private case class PosInt(value: Int)

  private lazy val am = AssessmentMethod("", "", "")
  private lazy val fa = FocusAreaPreview("")
  private lazy val ld = LocalDate.of(1998, 5, 9)
  private lazy val sp = PO("", 0, ld, ld, None, Nil, "")
  private lazy val creditPointFactor = 30

  private def method(percentage: Option[Double]) =
    AssessmentMethodEntry(am, percentage, Nil)

  private def ectsContrib(value: Double) =
    ECTSFocusAreaContribution(fa, value, "", "")

  private def prerequisiteEntry(modules: List[UUID]) =
    ParsedPrerequisiteEntry("", modules, Nil)

  private def poOpt(module: UUID) =
    ParsedPOOptional(sp, None, module, partOfCatalog = false, Nil)

  val m1 = Module(UUID.randomUUID, "t1", "m1")
  val m2 = Module(UUID.randomUUID, "t1", "m2")
  val m3 = Module(UUID.randomUUID, "t1", "m3")
  val modules = List(m1, m2, m3)

  def lookup(module: UUID): Option[Module] =
    modules.find(_.id == module)

  "A Metadata Validator" when {
    "flatMap a validator" in {
      def posInt: Validator[Int, PosInt] =
        Validator(int => Either.cond(int > 0, PosInt(int), List("must be pos")))
      def grade(int: PosInt): Validator[Int, String] =
        Validator { _ =>
          int.value match {
            case 1 => Right("good")
            case 2 => Right("ok")
            case 3 => Right("failed")
            case _ => Left(List("wrong grade"))
          }
        }
      val validator =
        posInt.flatMap((_, int) => grade(int).map((_, string) => (int, string)))
      assert(validator.validate(1).value == (PosInt(1), "good"))
      assert(validator.validate(0).left.value == List("must be pos"))
      assert(validator.validate(4).left.value == List("wrong grade"))
    }

    "validating non empty strings" in {
      assert(nonEmptyStringValidator("value").validate("test").value == "test")
      assert(
        nonEmptyStringValidator("value").validate("").left.value == List(
          "value must be set, but was empty"
        )
      )
    }

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

    "validating modules" should {
      "pass if all modules are found" in {
        assert(
          moduleValidator("module", lookup)
            .validate(List(m1.id, m2.id))
            .value == List(
            m1,
            m2
          )
        )
        assert(
          moduleValidator("module", lookup).validate(List(m1.id)).value == List(
            m1
          )
        )
        assert(
          moduleValidator("module", lookup).validate(Nil).value.isEmpty
        )
      }

      "fail if one module can't be found" in {
        val m4 = UUID.randomUUID
        val m5 = UUID.randomUUID
        assert(
          moduleValidator("module", lookup)
            .validate(List(m1.id, m4, m5))
            .left
            .value == List(
            s"module in 'module' not found: $m4",
            s"module in 'module' not found: $m5"
          )
        )
      }
    }

    "validating prerequisites" should {
      "pass if prerequisites are empty" in {
        assert(
          prerequisitesEntryValidator("prerequisites", lookup)
            .validate(None)
            .value
            .isEmpty
        )
      }

      "pass if modules defined in prerequisiteEntry are found" in {
        assert(
          prerequisitesEntryValidator("prerequisites", lookup)
            .validate(Some(prerequisiteEntry(List(m1.id, m2.id))))
            .value
            .value == PrerequisiteEntry("", List(m1, m2), Nil)
        )
        assert(
          prerequisitesEntryValidator("prerequisites", lookup)
            .validate(Some(prerequisiteEntry(List(m1.id))))
            .value
            .value == PrerequisiteEntry("", List(m1), Nil)
        )
        assert(
          prerequisitesEntryValidator("prerequisites", lookup)
            .validate(Some(prerequisiteEntry(Nil)))
            .value
            .value == PrerequisiteEntry("", Nil, Nil)
        )
      }

      "fail if modules defined in prerequisiteEntry are not found" in {
        val id = UUID.randomUUID
        assert(
          prerequisitesEntryValidator("prerequisites", lookup)
            .validate(Some(prerequisiteEntry(List(id))))
            .left
            .value == List(s"module in 'prerequisites' not found: $id")
        )
      }

      "pass validating prerequisites" in {
        val random = UUID.randomUUID
        val random2 = UUID.randomUUID
        val random3 = UUID.randomUUID
        assert(
          prerequisitesValidator(lookup)
            .validate(
              ParsedPrerequisites(
                Some(prerequisiteEntry(List(m1.id))),
                Some(prerequisiteEntry(List(m2.id)))
              )
            )
            .value == Prerequisites(
            Some(PrerequisiteEntry("", List(m1), Nil)),
            Some(PrerequisiteEntry("", List(m2), Nil))
          )
        )
        assert(
          prerequisitesValidator(lookup)
            .validate(
              ParsedPrerequisites(
                Some(prerequisiteEntry(List(m1.id))),
                None
              )
            )
            .value == Prerequisites(
            Some(PrerequisiteEntry("", List(m1), Nil)),
            None
          )
        )
        assert(
          prerequisitesValidator(lookup)
            .validate(ParsedPrerequisites(None, None))
            .value == Prerequisites(None, None)
        )
        assert(
          prerequisitesValidator(lookup)
            .validate(
              ParsedPrerequisites(
                Some(prerequisiteEntry(List(m1.id))),
                Some(prerequisiteEntry(List(random)))
              )
            )
            .left
            .value == List(
            s"module in 'required prerequisites' not found: $random"
          )
        )
        assert(
          prerequisitesValidator(lookup)
            .validate(
              ParsedPrerequisites(
                Some(prerequisiteEntry(List(random, random2))),
                Some(prerequisiteEntry(List(random3)))
              )
            )
            .left
            .value == List(
            s"module in 'recommended prerequisites' not found: $random",
            s"module in 'recommended prerequisites' not found: $random2",
            s"module in 'required prerequisites' not found: $random3"
          )
        )
      }
    }

    "validating workload" should {
      "pass by setting self study and total value" in {
        assert(
          workloadValidator(creditPointFactor)
            .validate((ParsedWorkload(10, 10, 0, 0, 10, 0), ECTS(2, Nil)))
            .value == Workload(10, 10, 0, 0, 10, 0, 30, 60)
        )
        assert(
          workloadValidator(creditPointFactor)
            .validate((ParsedWorkload(0, 0, 0, 0, 0, 0), ECTS(2, Nil)))
            .value == Workload(0, 0, 0, 0, 0, 0, 60, 60)
        )
        assert(
          workloadValidator(creditPointFactor)
            .validate((ParsedWorkload(0, 0, 0, 0, 0, 0), ECTS(0, Nil)))
            .value == Workload(0, 0, 0, 0, 0, 0, 0, 0)
        )
      }
    }

    "validating po optionals" should {
      "pass if modules can be found" in {
        assert(
          poOptionalValidator(lookup)
            .validate(List(poOpt(m1.id)))
            .value == List(
            POOptional(sp, None, m1, partOfCatalog = false, Nil)
          )
        )
        assert(
          poOptionalValidator(lookup)
            .validate(List(poOpt(m1.id), poOpt(m2.id)))
            .value == List(
            POOptional(sp, None, m1, partOfCatalog = false, Nil),
            POOptional(sp, None, m2, partOfCatalog = false, Nil)
          )
        )
        assert(poOptionalValidator(lookup).validate(Nil).value == Nil)
      }

      "fail if modules cant be found" in {
        val random = UUID.randomUUID
        assert(
          poOptionalValidator(lookup)
            .validate(List(poOpt(m1.id), poOpt(random)))
            .left
            .value == List(s"module in 'po optional' not found: $random")
        )
      }

      "handle pos validation" in {
        val random = UUID.randomUUID
        posValidator(lookup)
          .validate(ParsedPOs(Nil, List(poOpt(m1.id))))
          .value == POs(
          Nil,
          List(POOptional(sp, None, m1, partOfCatalog = false, Nil))
        )
        posValidator(lookup)
          .validate(ParsedPOs(Nil, Nil))
          .value == POs(Nil, Nil)
        posValidator(lookup)
          .validate(ParsedPOs(Nil, List(poOpt(random))))
          .left
          .value == List(s"module not found: $random")
      }
    }

    "validating module relations" should {
      "skip if there is no module relation" in {
        assert(moduleRelationValidator(lookup).validate(None).value.isEmpty)
      }

      "pass if parent is found" in {
        assert(
          moduleRelationValidator(lookup)
            .validate(Some(ParsedModuleRelation.Child(m1.id)))
            .value
            .value == ModuleRelation.Child(m1)
        )
      }

      "fail if parent is not found" in {
        val random = UUID.randomUUID
        assert(
          moduleRelationValidator(lookup)
            .validate(Some(ParsedModuleRelation.Child(random)))
            .left
            .value == List(s"module in 'module relation' not found: $random")
        )
      }

      "pass if children are found" in {
        assert(
          moduleRelationValidator(lookup)
            .validate(Some(ParsedModuleRelation.Parent(List(m1.id, m2.id))))
            .value
            .value == ModuleRelation.Parent(List(m1, m2))
        )
        assert(
          moduleRelationValidator(lookup)
            .validate(Some(ParsedModuleRelation.Parent(Nil)))
            .value
            .value == ModuleRelation.Parent(Nil)
        )
      }

      "fail if one child is not found" in {
        val random = UUID.randomUUID
        assert(
          moduleRelationValidator(lookup)
            .validate(Some(ParsedModuleRelation.Parent(List(m1.id, random))))
            .left
            .value == List(s"module in 'module relation' not found: $random")
        )
      }
    }

    "validating metadata" should {
      "pass if everything is fine" in {
        val ivm1: ParsedMetadata = ParsedMetadata(
          UUID.randomUUID(),
          "title",
          "abbrev",
          ModuleType("", "", ""),
          Some(ParsedModuleRelation.Child(m1.id)),
          Left(1),
          Language("", "", ""),
          1,
          Season("", "", ""),
          Responsibilities(Nil, Nil),
          AssessmentMethods(
            List(method(Some(50)), method(Some(50))),
            List(method(None))
          ),
          ParsedWorkload(5, 0, 0, 0, 0, 0),
          ParsedPrerequisites(None, None),
          Status("", "", ""),
          Location("", "", ""),
          ParsedPOs(
            List(POMandatory(sp, None, List(1), Nil)),
            List(
              ParsedPOOptional(sp, None, m2.id, partOfCatalog = false, List(2))
            )
          ),
          Some(Participants(10, 20)),
          Nil,
          Nil,
          Nil
        )
        val vm1 = Metadata(
          ivm1.id,
          ivm1.title,
          ivm1.abbrev,
          ivm1.kind,
          Some(ModuleRelation.Child(m1)),
          ECTS(1, Nil),
          ivm1.language,
          ivm1.duration,
          ivm1.season,
          ivm1.responsibilities,
          ivm1.assessmentMethods,
          Workload(5, 0, 0, 0, 0, 0, 5, 10),
          Prerequisites(None, None),
          ivm1.status,
          ivm1.location,
          POs(
            List(POMandatory(sp, None, List(1), Nil)),
            List(POOptional(sp, None, m2, partOfCatalog = false, List(2)))
          ),
          Some(Participants(10, 20)),
          Nil,
          Nil,
          Nil
        )

        val res = validateMany(Seq(ivm1), 10, lookup).head.value
        assert(res.id == vm1.id)
        assert(res.title == vm1.title)
        assert(res.abbrev == vm1.abbrev)
        assert(res.kind == vm1.kind)
        assert(res.relation == vm1.relation)
        assert(res.ects == vm1.ects)
        assert(res.language == vm1.language)
        assert(res.duration == vm1.duration)
        assert(res.season == vm1.season)
        assert(res.responsibilities == vm1.responsibilities)
        assert(res.assessmentMethods == vm1.assessmentMethods)
        assert(res.workload == vm1.workload)
        assert(res.prerequisites == vm1.prerequisites)
        assert(res.status == vm1.status)
        assert(res.location == vm1.location)
        assert(res.validPOs == vm1.validPOs)
        assert(res.participants == vm1.participants)
        assert(res.competences == vm1.competences)
        assert(res.globalCriteria == vm1.globalCriteria)
        assert(res.taughtWith == vm1.taughtWith)
      }

      "fail if something is invalid" in {
        val random = UUID.randomUUID
        val ivm1: ParsedMetadata = ParsedMetadata(
          UUID.randomUUID(),
          "",
          "abbrev",
          ModuleType("", "", ""),
          Some(ParsedModuleRelation.Child(random)),
          Left(1),
          Language("", "", ""),
          1,
          Season("", "", ""),
          Responsibilities(Nil, Nil),
          AssessmentMethods(
            List(method(Some(50)), method(Some(50))),
            List(method(None))
          ),
          ParsedWorkload(5, 0, 0, 0, 0, 0),
          ParsedPrerequisites(None, None),
          Status("", "", ""),
          Location("", "", ""),
          ParsedPOs(
            List(POMandatory(sp, None, List(1), Nil)),
            List(
              ParsedPOOptional(sp, None, m1.id, partOfCatalog = false, List(2))
            )
          ),
          Some(Participants(20, 15)),
          Nil,
          Nil,
          Nil
        )

        assert(
          validateMany(Seq(ivm1), 10, lookup).head.left.value == List(
            "title must be set, but was empty",
            "participants min must be lower than max. min: 20, max: 15",
            s"module in 'module relation' not found: $random"
          )
        )
      }
    }
  }
}
