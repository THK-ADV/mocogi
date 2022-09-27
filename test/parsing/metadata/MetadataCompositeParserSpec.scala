package parsing.metadata

import helper._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parser.ParsingError
import parsing.compendium.FakeSeasons
import parsing.types._
import parsing.{ParserSpecHelper, withFile0}

import java.util.UUID

final class MetadataCompositeParserSpec
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
    with FakePersons
    with FakeFocusAreas
    with FakeCompetences
    with FakeGlobalCriteria {

  val parser = app.injector.instanceOf(classOf[MetadataCompositeParser])

  val versionSchemeParser = parser.versionSchemeParser
  val metadataParser = parser.parser

  "A Metadata Composite Parser" should {

    "parse a version scheme if the scheme is valid" in {
      val input = "v1s\n"
      val (res, rest) = versionSchemeParser.parse(input)
      assert(rest == "\n")
      assert(res.value == VersionScheme(1, "s"))

      val input1 = "v1.5s\n"
      val (res1, rest1) = versionSchemeParser.parse(input1)
      assert(rest1 == "\n")
      assert(res1.value == VersionScheme(1.5, "s"))
    }

    "fail parsing if the version scheme has no number" in {
      val input = "vs\n"
      val (res, rest) = versionSchemeParser.parse(input)
      val ParsingError(expected, found) = res.left.value
      assert(rest == input)
      assert(expected == "a double")
      assert(found == "s\n")
    }

    "fail parsing if the version scheme has no label" in {
      val input = "v1\n"
      val (res, rest) = versionSchemeParser.parse(input)
      val e = res.left.value
      assert(rest == input)
      assert(e.expected == "a given prefix")
      assert(e.found == "\n")
    }

    "fail if the version scheme is not found" in {
      val input = "---v1x\n---"
      val (res, rest) = metadataParser.parse(input)
      val e = res.left.value
      assert(rest == input)
      assert(e.expected == "unknown version scheme v1.0x")
      assert(e.found == "---")
    }

    "fail if version scheme is missing" in {
      val input = "---\n---"
      val (res, rest) = metadataParser.parse(input)
      val e = res.left.value
      assert(rest == input)
      assert(e.expected == "v")
      assert(e.found == "\n---")
    }

    "parse different flavours of metadata" should {
      "a juicy one" in {
        val (res, rest) =
          withFile0("test/parsing/res/metadata1.yaml")(
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
        assert(metadata.relation.contains(ModuleRelation.Child("inf")))
        assert(metadata.credits.value == 5)
        assert(metadata.language == Language("de", "Deutsch", "--"))
        assert(metadata.duration == 1)
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
        assert(metadata.workload == Workload(36, 0, 18, 18, 0, 0))
        assert(
          metadata.recommendedPrerequisites.value == Prerequisites(
            "",
            List("ap1", "ap2", "ma1"),
            Nil
          )
        )
        assert(metadata.requiredPrerequisites.isEmpty)
        assert(metadata.status == Status("active", "Aktiv", "--"))
        assert(
          metadata.location == Location("gm", "Gummersbach", "--")
        )
        assert(
          metadata.poMandatory == List(
            POMandatory("ai2", List(3), Nil)
          )
        )
        assert(
          metadata.poOptional == List(
            POOptional("wi4", "wpf", partOfCatalog = false, List(3))
          )
        )
        assert(
          metadata.participants.value == Participants(4, 20)
        )
        assert(
          metadata.competences == List(
            Competence(
              "analyze-domains",
              "Analyze Domains",
              "...",
              "Analyze Domains",
              "..."
            ),
            Competence(
              "model-systems",
              "Model Systems",
              "...",
              "Model Systems",
              "..."
            )
          )
        )
        assert(
          metadata.globalCriteria == List(
            GlobalCriteria(
              "internationalization",
              "Internationalisierung",
              "...",
              "Internationalization",
              "..."
            ),
            GlobalCriteria(
              "digitization",
              "Digitalisierung",
              "...",
              "Digitization",
              "..."
            )
          )
        )
      }

      "another juicy one" in {
        val (res, rest) =
          withFile0("test/parsing/res/metadata2.yaml")(
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
        assert(metadata.credits.value == 2.5)
        assert(metadata.language == Language("en", "Englisch", "--"))
        assert(metadata.duration == 1)
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
        assert(metadata.workload == Workload(30, 0, 10, 10, 0, 0))
        assert(metadata.recommendedPrerequisites.isEmpty)
        assert(metadata.requiredPrerequisites.isEmpty)
        assert(metadata.status == Status("active", "Aktiv", "--"))
        assert(
          metadata.location == Location("gm", "Gummersbach", "--")
        )
        assert(
          metadata.poMandatory == List(
            POMandatory("ai2", List(4), Nil),
            POMandatory("mi4", List(4), Nil),
            POMandatory("itm2", List(4), Nil)
          )
        )
        assert(metadata.poOptional.isEmpty)
        assert(metadata.participants.value == Participants(4, 20))
        assert(metadata.competences.isEmpty)
        assert(metadata.globalCriteria.isEmpty)
        assert(rest.isEmpty)
      }
    }
  }
}
