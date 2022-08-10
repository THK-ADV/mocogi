package parsing.metadata

import helper._
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.compendium.FakeSeasons
import parsing.types._
import parsing.{ParserSpecHelper, withFile0}

import java.util.UUID

class THKV1ParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeLocations
    with FakeLanguages
    with FakeStatus
    with FakeAssessmentMethod
    with FakeModuleTypes
    with FakeSeasons
    with FakePersons {

  val parser = app.injector.instanceOf(classOf[THKV1Parser])

  val moduleCodeParser = parser.moduleCodeParser
  val moduleTitleParser = parser.moduleTitleParser
  val moduleAbbrevParser = parser.moduleAbbrevParser
  val creditPointsParser = parser.creditPointsParser
  val durationParser = parser.durationParser
  val semesterParser = parser.semesterParser
  val metadataParser = parser.parser

  "A Metadata Parser" when {
    "parse module code" should {
      "return a valid uuid" in {
        val id = UUID.fromString("00895144-30e4-4bd2-b800-bb706686d950")
        val (res, rest) = moduleCodeParser.parse(
          "module_code: 00895144-30e4-4bd2-b800-bb706686d950\n"
        )
        assert(res.value == id)
        assert(rest.isEmpty)
      }

      "fail if the uuid is invalid" in {
        assertError(
          moduleCodeParser,
          "module_code: 123\n",
          "uuid",
          Some("")
        )
      }
    }

    "parse module title" should {

      "return the module title" in {
        val (res1, rest1) =
          moduleTitleParser.parse("module_title: Algorithmik\n")
        assert(res1.value == "Algorithmik")
        assert(rest1.isEmpty)
      }

      "return the module title even if there is whitespace" in {
        val (res2, rest2) =
          moduleTitleParser.parse("module_title:      Algorithmik   \n")
        assert(res2.value == "Algorithmik")
        assert(rest2.isEmpty)
      }
    }

    "parse module abbreviation" in {
      val (res, rest) = moduleAbbrevParser.parse("module_abbrev: ALG\n")
      assert(res.value == "ALG")
      assert(rest.isEmpty)
    }

    "parse credit points" in {
      val (res1, rest1) = creditPointsParser.parse("credit_points: 5")
      assert(res1.value == 5)
      assert(rest1.isEmpty)
      val (res2, rest2) = creditPointsParser.parse("credit_points: -5")
      assert(res2.value == -5)
      assert(rest2.isEmpty)
      val (res3, rest3) = creditPointsParser.parse("credit_points: 2.5")
      assert(res3.value == 2.5)
      assert(rest3.isEmpty)
    }

    "parse duration of module" in {
      val (res1, rest1) = durationParser.parse("duration_of_module: 1")
      assert(res1.value == 1)
      assert(rest1.isEmpty)

      val (res2, rest2) = durationParser.parse("duration_of_module: -1")
      assert(res2.value == -1)
      assert(rest2.isEmpty)
    }

    "parse recommended semester" should {

      "return a valid semester if the input is an integer" in {
        val (res, rest) = semesterParser.parse("recommended_semester: 3")
        assert(res.value == 3)
        assert(rest.isEmpty)
      }

      "fail if the semester is a literal" in {
        assertError(
          semesterParser,
          "recommended_semester: Wintersemester",
          "an integer",
          Some("Wintersemester")
        )
      }
    }

    "parse different flavours of metadata" should {
      "a juicy one" in {
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
        assert(metadata.kind == ModuleType("mandatory", "Pflicht", "--"))
        assert(metadata.relation.contains(ModuleRelation.Child("inf")))
        assert(metadata.credits == 5)
        assert(metadata.language == Language("de", "Deutsch", "--"))
        assert(metadata.duration == 1)
        assert(metadata.recommendedSemester == 3)
        assert(metadata.frequency == Season("ws", "Wintersemester", "--"))
        assert(
          metadata.responsibilities == Responsibilities(
            List(
              Person("ald", "Dobrynin", "Alexander", "M.Sc.", "F10")
            ),
            List(
              Person("ald", "Dobrynin", "Alexander", "M.Sc.", "F10"),
              Person("abe", "Bertels", "Anja", "B.Sc.", "F10")
            )
          )
        )
        assert(
          metadata.assessmentMethods == List(
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
              None
            )
          )
        )
        assert(metadata.workload == Workload(150, 36, 0, 18, 18, 78))
        assert(metadata.recommendedPrerequisites == List("ap1", "ap2", "ma1"))
        assert(metadata.requiredPrerequisites == List.empty)
        assert(metadata.status == Status("active", "Aktiv", "--"))
        assert(metadata.location == Location("gm", "Gummersbach", "--"))
        assert(metadata.po == List("AI2"))
      }

      "another juicy one" in {
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
        assert(metadata.kind == ModuleType("wpf", "Wahlpflichtfach", "--"))
        assert(metadata.relation.isEmpty)
        assert(metadata.credits == 2.5)
        assert(metadata.language == Language("en", "Englisch", "--"))
        assert(metadata.duration == 1)
        assert(metadata.recommendedSemester == 4)
        assert(metadata.frequency == Season("ss", "Sommersemester", "--"))
        assert(
          metadata.responsibilities == Responsibilities(
            List(
              Person("ald", "Dobrynin", "Alexander", "M.Sc.", "F10")
            ),
            List(
              Person("ald", "Dobrynin", "Alexander", "M.Sc.", "F10")
            )
          )
        )
        assert(
          metadata.assessmentMethods == List(
            AssessmentMethodPercentage(
              AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
              Some(70)
            ),
            AssessmentMethodPercentage(
              AssessmentMethod("practical-report", "Praktikumsbericht", "--"),
              Some(30)
            )
          )
        )
        assert(metadata.workload == Workload(150, 30, 0, 10, 10, 100))
        assert(metadata.recommendedPrerequisites == List.empty)
        assert(metadata.requiredPrerequisites == List.empty)
        assert(metadata.status == Status("active", "Aktiv", "--"))
        assert(metadata.location == Location("gm", "Gummersbach", "--"))
        assert(metadata.po == List("AI2", "MI4", "ITM2"))
        assert(rest.isEmpty)
      }
    }
  }
}
