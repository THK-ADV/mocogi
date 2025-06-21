package parsing.metadata

import java.util.UUID

import cats.data.NonEmptyList
import helper.*
import models.core.*
import models.core.ExamPhases.ExamPhase
import models.EmploymentType.WMA
import models.ModuleWorkload
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import org.scalatest.OptionValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.types.*
import parsing.withFile0
import parsing.ParserSpecHelper

class THKV1ParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with OptionValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeLocations
    with FakeLanguages
    with FakeStatus
    with FakeAssessmentMethod
    with FakeModuleTypes
    with FakeSeasons
    with FakeIdentities
    with FakePOs
    with FakeSpecializations {

  import THKV1Parser._
  val parser = app.injector.instanceOf(classOf[THKV1Parser])

  val moduleCodeParser   = idParser
  val moduleTitleParser  = titleParser
  val moduleAbbrevParser = abbreviationParser
  val metadataParser     = parser.parser

  "A Metadata Parser" when {
    "parse module code" should {
      "return a valid uuid" in {
        val id = UUID.fromString("00895144-30e4-4bd2-b800-bb706686d950")
        val (res, rest) = moduleCodeParser.parse(
          "id: 00895144-30e4-4bd2-b800-bb706686d950\n"
        )
        assert(res.value == id)
        assert(rest.isEmpty)
      }

      "fail if the uuid is invalid" in {
        assertError(
          moduleCodeParser,
          "id: 123\n",
          "uuid",
          Some("")
        )
      }
    }

    "parse module title" should {

      "return the module title" in {
        val (res1, rest1) =
          moduleTitleParser.parse("title: Algorithmik\n")
        assert(res1.value == "Algorithmik")
        assert(rest1.isEmpty)
      }

      "return the module title even if there is whitespace" in {
        val (res2, rest2) =
          moduleTitleParser.parse("title:      Algorithmik   \n")
        assert(res2.value == "Algorithmik")
        assert(rest2.isEmpty)
      }
    }

    "parse module abbreviation" in {
      val (res, rest) = moduleAbbrevParser.parse("abbreviation: ALG\n")
      assert(res.value == "ALG")
      assert(rest.isEmpty)
    }

    "parse duration of module" in {
      val (res1, rest1) = durationParser.parse("duration: 1")
      assert(res1.value == 1)
      assert(rest1.isEmpty)
    }

    "parse different flavours of metadata" should {
      "thkv1metadata1.yaml" in {
        val (res, rest) =
          withFile0("test/parsing/res/thkv1metadata1.yaml")(
            metadataParser.parse
          )
        assert(rest.isEmpty)
        val metadata = res.value
        assert(
          metadata.id == UUID.fromString("00895144-30e4-4bd2-b800-bb706686d950")
        )
        assert(metadata.title == "Algorithmik")
        assert(metadata.abbrev == "ALG")
        assert(metadata.kind == ModuleType("module", "Modul", "--"))
        assert(
          metadata.relation.contains(
            ParsedModuleRelation.Child(
              UUID.fromString("3bd4ee93-b921-4d52-9223-04be3bb13676")
            )
          )
        )
        assert(metadata.credits == 5)
        assert(metadata.language == ModuleLanguage("de", "Deutsch", "--"))
        assert(metadata.duration == 1)
        assert(metadata.season == Season("ws", "Wintersemester", "--"))
        assert(
          metadata.responsibilities == ModuleResponsibilities(
            NonEmptyList.one(
              Identity.Person(
                "ald",
                "Dobrynin",
                "Alexander",
                "M.Sc.",
                List("f10"),
                "ad",
                Some("ald"),
                isActive = true,
                WMA,
                None,
                None
              )
            ),
            NonEmptyList.of(
              Identity.Person(
                "ald",
                "Dobrynin",
                "Alexander",
                "M.Sc.",
                List("f10"),
                "ad",
                Some("ald"),
                isActive = true,
                WMA,
                None,
                None
              ),
              Identity.Person(
                "abe",
                "Bertels",
                "Anja",
                "B.Sc.",
                List("f10"),
                "ab",
                Some("abe"),
                isActive = true,
                WMA,
                None,
                None
              )
            )
          )
        )
        assert(
          metadata.assessmentMethods == ModuleAssessmentMethods(
            List(
              ModuleAssessmentMethodEntry(
                AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
                None,
                Nil
              )
            ),
            Nil
          )
        )
        assert(metadata.workload == ModuleWorkload(36, 0, 18, 18, 0, 0))
        assert(
          metadata.prerequisites == ParsedPrerequisites(
            Some(
              ParsedPrerequisiteEntry(
                "",
                List(
                  UUID.fromString("ce09539e-fc0a-4c74-b85d-40a293998bb4"),
                  UUID.fromString("d4365c35-a3aa-4dab-b6a3-e17449269055"),
                  UUID.fromString("84223dc5-69b9-4dbb-a6ea-45bf5e9672e3")
                ),
                Nil
              )
            ),
            None
          )
        )
        assert(metadata.status == ModuleStatus("active", "Aktiv", "--"))
        assert(metadata.location == ModuleLocation("gm", "Gummersbach", "--"))
        assert(
          metadata.pos == ParsedPOs(
            List(ModulePOMandatory(inf1, None, List(3))),
            List(
              ParsedPOOptional(
                wi1,
                None,
                UUID.fromString("d1cecfbc-a314-42f6-99b3-be92f22c3295"),
                partOfCatalog = false,
                List(3)
              )
            )
          )
        )
        assert(
          metadata.participants.value == ModuleParticipants(4, 20)
        )
        assert(
          metadata.competences == List(
            ModuleCompetence(
              "analyze-domains",
              "",
              "",
              "",
              ""
            ),
            ModuleCompetence(
              "model-systems",
              "",
              "",
              "",
              ""
            )
          )
        )
        assert(
          metadata.globalCriteria == List(
            ModuleGlobalCriteria(
              "internationalization",
              "",
              "",
              "",
              ""
            ),
            ModuleGlobalCriteria(
              "digitization",
              "",
              "",
              "",
              ""
            )
          )
        )
        assert(metadata.taughtWith.isEmpty)
        assert(metadata.examiner.first == fakeIdentities.find(_.id == "ald").value)
        assert(metadata.examiner.second == fakeIdentities.find(_.id == "abe").value)
        assert(metadata.examPhases == NonEmptyList.one(ExamPhase.none))
      }

      "thkv1metadata2.yaml" in {
        val (res, rest) =
          withFile0("test/parsing/res/thkv1metadata2.yaml")(
            metadataParser.parse
          )
        val metadata = res.value
        assert(
          metadata.id == UUID.fromString("00895144-30e4-4bd2-b800-bb706686d950")
        )
        assert(metadata.title == "IOS Stuff")
        assert(metadata.abbrev == "IOS")
        assert(metadata.kind == ModuleType("module", "Modul", "--"))
        assert(metadata.relation.isEmpty)
        assert(metadata.credits == 9.5)
        assert(metadata.language == ModuleLanguage("en", "Englisch", "--"))
        assert(metadata.duration == 1)
        assert(metadata.season == Season("ss", "Sommersemester", "--"))
        assert(
          metadata.responsibilities == ModuleResponsibilities(
            NonEmptyList.one(
              Identity.Person(
                "ald",
                "Dobrynin",
                "Alexander",
                "M.Sc.",
                List("f10"),
                "ad",
                Some("ald"),
                isActive = true,
                WMA,
                None,
                None
              )
            ),
            NonEmptyList.one(
              Identity.Person(
                "ald",
                "Dobrynin",
                "Alexander",
                "M.Sc.",
                List("f10"),
                "ad",
                Some("ald"),
                isActive = true,
                WMA,
                None,
                None
              )
            )
          )
        )
        assert(
          metadata.assessmentMethods == ModuleAssessmentMethods(
            List(
              ModuleAssessmentMethodEntry(
                AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
                Some(70),
                List(AssessmentMethod("practical", "Praktikum", "--"))
              ),
              ModuleAssessmentMethodEntry(
                AssessmentMethod("practical-report", "Praktikumsbericht", "--"),
                Some(30),
                Nil
              )
            ),
            List(
              ModuleAssessmentMethodEntry(
                AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
                None,
                Nil
              )
            )
          )
        )
        assert(metadata.workload == ModuleWorkload(30, 0, 10, 10, 0, 0))
        assert(metadata.prerequisites == ParsedPrerequisites(None, None))
        assert(metadata.status == ModuleStatus("active", "Aktiv", "--"))
        assert(metadata.location == ModuleLocation("gm", "Gummersbach", "--"))
        assert(
          metadata.pos == ParsedPOs(
            List(
              ModulePOMandatory(inf1, None, List(4)),
              ModulePOMandatory(mi1, None, List(4)),
              ModulePOMandatory(itm1, None, List(4))
            ),
            Nil
          )
        )
        assert(metadata.participants.value == ModuleParticipants(4, 20))
        assert(metadata.competences.isEmpty)
        assert(metadata.globalCriteria.isEmpty)
        assert(
          metadata.taughtWith == List(
            UUID.fromString("d1cecfbc-a314-42f6-99b3-be92f22c3295")
          )
        )
        assert(rest.isEmpty)
        assert(metadata.examiner.first == Identity.NN)
        assert(metadata.examiner.second == Identity.NN)
        assert(metadata.examPhases == NonEmptyList.one(ExamPhase.none))
      }

      "thkv1metadata3.yaml" in {
        val (res, rest) =
          withFile0("test/parsing/res/thkv1metadata3.yaml")(
            metadataParser.parse
          )
        val metadata = res.value
        assert(
          metadata.id == UUID.fromString("00895144-30e4-4bd2-b800-bb706686d950")
        )
        assert(res.value.pos.mandatory.isEmpty)
        assert(res.value.pos.optional.size == 1)
        assert(rest.isEmpty)
        assert(metadata.examiner.first == Identity.NN)
        assert(metadata.examiner.second == Identity.NN)
        assert(metadata.examPhases == NonEmptyList.one(ExamPhase.none))
      }
    }
  }
}
