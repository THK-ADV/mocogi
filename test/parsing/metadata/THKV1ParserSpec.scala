package parsing.metadata

import helper._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.types._
import parsing.{ParserSpecHelper, withFile0}

import java.util.UUID

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
    with FakePersons
    with FakeFocusAreas
    with FakeGlobalCriteria
    with FakeCompetences
    with FakeStudyPrograms {

  val parser = app.injector.instanceOf(classOf[THKV1Parser])

  val moduleCodeParser = parser.idParser
  val moduleTitleParser = parser.titleParser
  val moduleAbbrevParser = parser.abbreviationParser
  val durationParser = parser.durationParser
  val metadataParser = parser.parser

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

      val (res2, rest2) = durationParser.parse("duration: -1")
      assert(res2.value == -1)
      assert(rest2.isEmpty)
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
          metadata.assessmentMethods == AssessmentMethods(
            List(
              AssessmentMethodEntry(
                AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
                None,
                Nil
              )
            ),
            Nil
          )
        )
        assert(metadata.workload == Workload(36, 0, 18, 18, 0, 0))
        assert(
          metadata.prerequisites == Prerequisites(
            Some(
              PrerequisiteEntry(
                "",
                List("ap1", "ap2", "ma1"),
                Nil
              )
            ),
            None
          )
        )
        assert(metadata.status == Status("active", "Aktiv", "--"))
        assert(metadata.location == Location("gm", "Gummersbach", "--"))
        assert(
          metadata.pos == POs(
            List(
              POMandatory(StudyProgram("ai2"), List(3), Nil)
            ),
            List(
              POOptional(
                StudyProgram("wi5"),
                "wpf",
                partOfCatalog = false,
                List(3)
              )
            )
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
        assert(metadata.taughtWith.isEmpty)
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
        assert(
          metadata.credits == ECTS(
            9.5,
            List(
              ECTSFocusAreaContribution(FocusArea("gak"), 3.5, ""),
              ECTSFocusAreaContribution(FocusArea("acs"), 6, "Text1\nText2\n")
            )
          )
        )
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
          metadata.assessmentMethods == AssessmentMethods(
            List(
              AssessmentMethodEntry(
                AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
                Some(70),
                List(AssessmentMethod("practical", "Praktikum", "--"))
              ),
              AssessmentMethodEntry(
                AssessmentMethod("practical-report", "Praktikumsbericht", "--"),
                Some(30),
                Nil
              )
            ),
            List(
              AssessmentMethodEntry(
                AssessmentMethod("written-exam", "Klausurarbeiten", "--"),
                None,
                Nil
              )
            )
          )
        )
        assert(metadata.workload == Workload(30, 0, 10, 10, 0, 0))
        assert(metadata.prerequisites == Prerequisites(None, None))
        assert(metadata.status == Status("active", "Aktiv", "--"))
        assert(metadata.location == Location("gm", "Gummersbach", "--"))
        assert(
          metadata.pos == POs(
            List(
              POMandatory(StudyProgram("ai2"), List(4), Nil),
              POMandatory(StudyProgram("mi4"), List(4), Nil),
              POMandatory(StudyProgram("itm2"), List(4), Nil)
            ),
            Nil
          )
        )
        assert(metadata.participants.value == Participants(4, 20))
        assert(metadata.competences.isEmpty)
        assert(metadata.globalCriteria.isEmpty)
        assert(metadata.taughtWith == List("pp-mi"))
        assert(rest.isEmpty)
      }
    }
  }
}
