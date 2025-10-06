package validation

import java.time.LocalDate
import java.util.UUID

import cats.data.NonEmptyList
import models.*
import models.core.*
import models.core.ExamPhases.ExamPhase
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import org.scalatest.OptionValues
import parsing.types.*
import validation.MetadataValidator.*

final class MetadataValidatorSpec extends AnyWordSpec with EitherValues with OptionValues {

  private case class PosInt(value: Int)

  private lazy val am = AssessmentMethod("", "", "")
  private lazy val ld = LocalDate.of(1998, 5, 9)
  private lazy val sp = PO("", 0, "", ld, None, 30)

  private def method(percentage: Option[Double]) =
    ModuleAssessmentMethodEntry(am, percentage, Nil)

  private def prerequisiteEntry(modules: List[UUID]) =
    ParsedPrerequisiteEntry("", modules, Nil)

  private def poOpt(module: UUID) =
    ParsedPOOptional(sp, None, module, partOfCatalog = false, Nil)

  val m1      = ModuleCore(UUID.randomUUID, "t1", "m1")
  val m2      = ModuleCore(UUID.randomUUID, "t1", "m2")
  val m3      = ModuleCore(UUID.randomUUID, "t1", "m3")
  val modules = List(m1, m2, m3)

  def lookup(module: UUID): Option[ModuleCore] =
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
        val am1 = ModuleAssessmentMethods(Nil)
        assert(assessmentMethodsValidator.validate(am1).value == am1)

        val am2 = ModuleAssessmentMethods(List(method(None)))
        assert(assessmentMethodsValidator.validate(am2).value == am2)

        val am3 = ModuleAssessmentMethods(List(method(Some(0))))
        assert(assessmentMethodsValidator.validate(am3).value == am3)
      }

      "pass if their percentage matches 100" in {
        val am1 = ModuleAssessmentMethods(List(method(Some(30)), method(Some(70))))
        assert(assessmentMethodsValidator.validate(am1).value == am1)

        val am2 = ModuleAssessmentMethods(List(method(Some(100))))
        assert(assessmentMethodsValidator.validate(am2).value == am2)
      }

      "fail if their percentage doesn't match 100" in {
        val am1 =
          ModuleAssessmentMethods(List(method(Some(10)), method(Some(10))))
        assert(
          assessmentMethodsValidator.validate(am1).left.value == List(
            "mandatory sum must be null or 100, but was 20.0"
          )
        )

        val am2 = ModuleAssessmentMethods(List(method(Some(50))))
        assert(
          assessmentMethodsValidator.validate(am2).left.value == List(
            "mandatory sum must be null or 100, but was 50.0"
          )
        )
      }
    }

    "validating participants" should {
      "pass if participants are not set" in {
        assert(participantsValidator.validate(None).value.isEmpty)
      }

      "pass if participants minimum is higher than 0" in {
        val p1 = ModuleParticipants(0, 10)
        assert(
          participantsValidator.validate(Some(p1)).value.value == p1
        )
      }

      "fail if participants minimum or maximum is lower than 0" in {
        val p1 = ModuleParticipants(-1, 10)
        assert(
          participantsValidator.validate(Some(p1)).left.value == List(
            "participants min must be positive, but was -1"
          )
        )

        val p2 = ModuleParticipants(0, -1)
        assert(
          participantsValidator.validate(Some(p2)).left.value == List(
            "participants max must be positive, but was -1",
            "participants min must be lower than max. min: 0, max: -1"
          )
        )
      }

      "fail if participants minimum is higher than max" in {
        val p1 = ModuleParticipants(10, 10)
        assert(
          participantsValidator.validate(Some(p1)).left.value == List(
            "participants min must be lower than max. min: 10, max: 10"
          )
        )

        val p2 = ModuleParticipants(11, 10)
        assert(
          participantsValidator.validate(Some(p2)).left.value == List(
            "participants min must be lower than max. min: 11, max: 10"
          )
        )
      }

      "pass if participants ranges are valid" in {
        val p1 = ModuleParticipants(5, 10)
        assert(
          participantsValidator.validate(Some(p1)).value.value == p1
        )
      }
    }

    "validating ects" should {
      "pass if ects value is already set" in {
        assert(ectsValidator.validate(5).value == ModuleECTS(5, Nil))
      }

      "fail if neither ects value nor contributions to focus areas are set" in {
        assert(
          ectsValidator.validate(0).left.value == List(
            "ects value must be set if contributions to focus areas are empty"
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
            .value == ModulePrerequisiteEntry("", List(m1, m2), Nil)
        )
        assert(
          prerequisitesEntryValidator("prerequisites", lookup)
            .validate(Some(prerequisiteEntry(List(m1.id))))
            .value
            .value == ModulePrerequisiteEntry("", List(m1), Nil)
        )
        assert(
          prerequisitesEntryValidator("prerequisites", lookup)
            .validate(Some(prerequisiteEntry(Nil)))
            .value
            .value == ModulePrerequisiteEntry("", Nil, Nil)
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
        val random  = UUID.randomUUID
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
            .value == ModulePrerequisites(
            Some(ModulePrerequisiteEntry("", List(m1), Nil)),
            Some(ModulePrerequisiteEntry("", List(m2), Nil))
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
            .value == ModulePrerequisites(
            Some(ModulePrerequisiteEntry("", List(m1), Nil)),
            None
          )
        )
        assert(
          prerequisitesValidator(lookup)
            .validate(ParsedPrerequisites(None, None))
            .value == ModulePrerequisites(None, None)
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
      "pass if allowed hours and self study are considered" in {
        assert(
          workloadValidator
            .validate((ModuleWorkload(10, 10, 0, 0, 10, 0), 2, Set(30)))
            .value == ModuleWorkload(10, 10, 0, 0, 10, 0)
        )
        assert(
          workloadValidator
            .validate((ModuleWorkload(60, 0, 0, 0, 0, 0), 2, Set(30)))
            .value == ModuleWorkload(60, 0, 0, 0, 0, 0)
        )
        assert(
          workloadValidator
            .validate((ModuleWorkload(50, 0, 0, 0, 0, 0), 2, Set(25, 30)))
            .value == ModuleWorkload(50, 0, 0, 0, 0, 0)
        )
        assert(
          workloadValidator
            .validate((ModuleWorkload(50, 0, 0, 0, 0, 0), 2, Set.empty))
            .value == ModuleWorkload(50, 0, 0, 0, 0, 0)
        )
        assert(workloadValidator.validate((ModuleWorkload(60, 0, 0, 0, 0, 0), 2, Set(25, 30))).isLeft)
        assert(workloadValidator.validate((ModuleWorkload(65, 0, 0, 0, 0, 0), 2, Set(30))).isLeft)
      }
    }

    "validating po optionals" should {
      "pass if modules can be found" in {
        assert(
          poOptionalValidator(lookup)
            .validate(List(poOpt(m1.id)))
            .value == List(
            ModulePOOptional(sp, None, m1, partOfCatalog = false, Nil)
          )
        )
        assert(
          poOptionalValidator(lookup)
            .validate(List(poOpt(m1.id), poOpt(m2.id)))
            .value == List(
            ModulePOOptional(sp, None, m1, partOfCatalog = false, Nil),
            ModulePOOptional(sp, None, m2, partOfCatalog = false, Nil)
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
        assert(
          posValidator(lookup)
            .validate(ParsedPOs(Nil, List(poOpt(m1.id))))
            .value == ModulePOs(
            Nil,
            List(ModulePOOptional(sp, None, m1, partOfCatalog = false, Nil))
          )
        )
        assert(
          posValidator(lookup)
            .validate(ParsedPOs(Nil, Nil))
            .value == ModulePOs(Nil, Nil)
        )
        assert(
          posValidator(lookup)
            .validate(ParsedPOs(Nil, List(poOpt(random))))
            .left
            .value == List(s"module in 'po optional' not found: $random")
        )
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
            .validate(
              Some(ParsedModuleRelation.Parent(NonEmptyList.of(m1.id, m2.id)))
            )
            .value
            .value == ModuleRelation.Parent(NonEmptyList.of(m1, m2))
        )
      }

      "fail if one child is not found" in {
        val random = UUID.randomUUID
        assert(
          moduleRelationValidator(lookup)
            .validate(
              Some(ParsedModuleRelation.Parent(NonEmptyList.of(m1.id, random)))
            )
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
          1,
          ModuleLanguage("", "", ""),
          1,
          Season("", "", ""),
          ModuleResponsibilities(
            NonEmptyList.one(Identity.Unknown("id", "label")),
            NonEmptyList.one(Identity.Unknown("id", "label"))
          ),
          ModuleAssessmentMethods(List(method(Some(50)), method(Some(50)))),
          Examiner(Identity.NN, Identity.NN),
          ExamPhase.all,
          ModuleWorkload(5, 0, 0, 0, 0, 0),
          ParsedPrerequisites(None, None),
          ModuleStatus("", "", ""),
          ModuleLocation("", "", ""),
          ParsedPOs(
            List(ModulePOMandatory(sp, None, List(1))),
            List(
              ParsedPOOptional(sp, None, m2.id, partOfCatalog = false, List(2))
            )
          ),
          Some(ModuleParticipants(10, 20)),
          Nil,
          None,
          None
        )
        val vm1 = Metadata(
          ivm1.id,
          ivm1.title,
          ivm1.abbrev,
          ivm1.kind,
          Some(ModuleRelation.Child(m1)),
          ModuleECTS(1, Nil),
          ivm1.language,
          ivm1.duration,
          ivm1.season,
          ivm1.responsibilities,
          ivm1.assessmentMethods,
          ivm1.examiner,
          ivm1.examPhases,
          ModuleWorkload(5, 0, 0, 0, 0, 0),
          ModulePrerequisites(None, None),
          ivm1.status,
          ivm1.location,
          ModulePOs(
            List(ModulePOMandatory(sp, None, List(1))),
            List(ModulePOOptional(sp, None, m2, partOfCatalog = false, List(2)))
          ),
          Some(ModuleParticipants(10, 20)),
          Nil,
          None,
          None
        )

        val res = validateMany(Seq(ivm1), lookup).head.value
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
        assert(res.pos == vm1.pos)
        assert(res.participants == vm1.participants)
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
          1,
          ModuleLanguage("", "", ""),
          1,
          Season("", "", ""),
          ModuleResponsibilities(
            NonEmptyList.one(Identity.Unknown("id", "label")),
            NonEmptyList.one(Identity.Unknown("id", "label"))
          ),
          ModuleAssessmentMethods(List(method(Some(50)), method(Some(50)))),
          Examiner(Identity.NN, Identity.NN),
          ExamPhase.all,
          ModuleWorkload(5, 0, 0, 0, 0, 0),
          ParsedPrerequisites(None, None),
          ModuleStatus("", "", ""),
          ModuleLocation("", "", ""),
          ParsedPOs(
            List(ModulePOMandatory(sp, None, List(1))),
            List(
              ParsedPOOptional(sp, None, m1.id, partOfCatalog = false, List(2))
            )
          ),
          Some(ModuleParticipants(20, 15)),
          Nil,
          None,
          None
        )
        assert(
          validateMany(Seq(ivm1), lookup).head.left.value == List(
            "title must be set, but was empty",
            "participants min must be lower than max. min: 20, max: 15",
            s"module in 'module relation' not found: $random"
          )
        )
      }
    }
  }
}
