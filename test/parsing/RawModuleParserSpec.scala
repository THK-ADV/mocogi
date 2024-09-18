package parsing

import cats.data.NonEmptyList
import models.*
import models.core.ExamPhases.ExamPhase
import models.core.Identity
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import parsing.types.ModuleParticipants

import java.util.UUID

final class RawModuleParserSpec
    extends AnyWordSpec
    with EitherValues
    with OptionValues {
  private val parser = RawModuleParser.parser

  "A Raw Module parser" should {
    "parse module1.md" in {
      val (res, rest) =
        withFile0("test/parsing/res/module1.md")(
          parser.parse
        )
      assert(rest.isEmpty)
      val ModuleProtocol(id, metadata, deContent, enContent) = res.value
      assert(
        id.value == UUID.fromString("00895144-30e4-4bd2-b800-bb706686d950")
      )
      assert(metadata.title == "Algorithmik")
      assert(metadata.abbrev == "ALG")
      assert(metadata.moduleType == "module")
      assert(
        metadata.moduleRelation.contains(
          ModuleRelationProtocol.Child(
            UUID.fromString("3bd4ee93-b921-4d52-9223-04be3bb13676")
          )
        )
      )
      assert(metadata.ects == 5)
      assert(metadata.language == "de")
      assert(metadata.duration == 1)
      assert(metadata.season == "ws")
      assert(metadata.moduleManagement == NonEmptyList.one("ald"))
      assert(metadata.lecturers == NonEmptyList.of("ald", "abe"))
      assert(
        metadata.assessmentMethods.mandatory == List(
          ModuleAssessmentMethodEntryProtocol("written-exam", None, Nil)
        )
      )
      assert(metadata.assessmentMethods.optional.isEmpty)
      assert(metadata.workload == ModuleWorkload(36, 0, 18, 18, 0, 0))
      assert(metadata.prerequisites.required.isEmpty)
      assert(
        metadata.prerequisites.recommended.contains(
          ModulePrerequisiteEntryProtocol(
            "",
            List(
              UUID.fromString("ce09539e-fc0a-4c74-b85d-40a293998bb4"),
              UUID.fromString("d4365c35-a3aa-4dab-b6a3-e17449269055"),
              UUID.fromString("84223dc5-69b9-4dbb-a6ea-45bf5e9672e3")
            ),
            Nil
          )
        )
      )
      assert(metadata.status == "active")
      assert(metadata.location == "gm")
      assert(
        metadata.po.mandatory == List(
          ModulePOMandatoryProtocol("inf1", None, List(3))
        )
      )
      assert(
        metadata.po.optional == List(
          ModulePOOptionalProtocol(
            "wi1",
            None,
            UUID.fromString("d1cecfbc-a314-42f6-99b3-be92f22c3295"),
            partOfCatalog = false,
            List(3)
          )
        )
      )
      assert(
        metadata.participants.value == ModuleParticipants(4, 20)
      )
      assert(metadata.competences == List("analyze-domains", "model-systems"))
      assert(
        metadata.globalCriteria == List(
          "internationalization",
          "digitization"
        )
      )
      assert(metadata.taughtWith.isEmpty)
      assert(deContent.learningOutcome == "Programmieren lernen")
      assert(enContent.learningOutcome == "Learn to code")
      assert(deContent.content == "- Klassen\n- Vererbung\n- Polymorphie")
      assert(enContent.content == "- Classes\n- Inheritance\n- Polymorphism")
      assert(deContent.teachingAndLearningMethods == "Slides, Whiteboard")
      assert(enContent.teachingAndLearningMethods == "")
      assert(
        deContent.recommendedReading == "Programmieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt"
      )
      assert(
        enContent.recommendedReading == "Programmieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt"
      )
      assert(deContent.particularities == "nichts")
      assert(enContent.particularities == "nothing")
      assert(metadata.examiner.first == Identity.NN.id)
      assert(metadata.examiner.second == Identity.NN.id)
      assert(metadata.examPhases.size == 1)
      assert(metadata.examPhases.head == ExamPhase.none.id)
    }

    "parse module2.md" in {
      val (res, rest) =
        withFile0("test/parsing/res/module2.md")(
          parser.parse
        )
      assert(rest.isEmpty)
      val ModuleProtocol(id, metadata, deContent, enContent) = res.value
      assert(
        id.value == UUID.fromString("00895144-30e4-4bd2-b800-bb706686d950")
      )
      assert(metadata.title == "IOS Stuff")
      assert(metadata.abbrev == "IOS")
      assert(metadata.moduleType == "module")
      assert(metadata.moduleRelation.isEmpty)
      assert(metadata.ects == 9.5)
      assert(metadata.language == "en")
      assert(metadata.duration == 1)
      assert(metadata.season == "ss")
      assert(metadata.moduleManagement == NonEmptyList.one("ald"))
      assert(metadata.lecturers == NonEmptyList.one("ald"))
      assert(
        metadata.assessmentMethods.mandatory == List(
          ModuleAssessmentMethodEntryProtocol(
            "written-exam",
            Some(70),
            List("practical")
          ),
          ModuleAssessmentMethodEntryProtocol("practical-report", Some(30), Nil)
        )
      )
      assert(
        metadata.assessmentMethods.optional == List(
          ModuleAssessmentMethodEntryProtocol("written-exam", None, Nil)
        )
      )
      assert(metadata.workload == ModuleWorkload(30, 0, 10, 10, 0, 0))
      assert(metadata.prerequisites.required.isEmpty)
      assert(metadata.prerequisites.recommended.isEmpty)
      assert(metadata.status == "active")
      assert(metadata.location == "gm")
      assert(
        metadata.po.mandatory == List(
          ModulePOMandatoryProtocol("inf1", None, List(4)),
          ModulePOMandatoryProtocol("mi1", None, List(4)),
          ModulePOMandatoryProtocol("itm1", None, List(4))
        )
      )
      assert(metadata.po.optional.isEmpty)
      assert(
        metadata.participants.value == ModuleParticipants(4, 20)
      )
      assert(metadata.competences.isEmpty)
      assert(metadata.globalCriteria.isEmpty)
      assert(
        metadata.taughtWith == List(
          UUID.fromString("d1cecfbc-a314-42f6-99b3-be92f22c3295")
        )
      )
      assert(deContent.learningOutcome == "")
      assert(enContent.learningOutcome == "")
      assert(deContent.content == "")
      assert(enContent.content == "")
      assert(deContent.teachingAndLearningMethods == "")
      assert(enContent.teachingAndLearningMethods == "")
      assert(deContent.recommendedReading == "")
      assert(enContent.recommendedReading == "")
      assert(deContent.particularities == "")
      assert(enContent.particularities == "")
      assert(metadata.examiner.first == "ald")
      assert(metadata.examiner.second == "abe")
      assert(metadata.examPhases == NonEmptyList.of("a", "b"))
    }

    "parse module3.md" in {
      val (res, rest) =
        withFile0("test/parsing/res/module3.md")(
          parser.parse
        )
      assert(rest.isEmpty)
      val ModuleProtocol(id, metadata, deContent, enContent) = res.value
      assert(
        id.value == UUID.fromString("00895144-30e4-4bd2-b800-bb706686d950")
      )
      assert(metadata.title == "Algorithmik")
      assert(metadata.abbrev == "ALG")
      assert(metadata.moduleType == "module")
      assert(
        metadata.moduleRelation.contains(
          ModuleRelationProtocol.Child(
            UUID.fromString("3bd4ee93-b921-4d52-9223-04be3bb13676")
          )
        )
      )
      assert(metadata.ects == 5)
      assert(metadata.language == "de")
      assert(metadata.duration == 1)
      assert(metadata.season == "ws")
      assert(metadata.moduleManagement == NonEmptyList.one("ald"))
      assert(metadata.lecturers == NonEmptyList.of("ald", "abe"))
      assert(
        metadata.assessmentMethods.mandatory == List(
          ModuleAssessmentMethodEntryProtocol("written-exam", None, Nil)
        )
      )

      assert(metadata.assessmentMethods.optional.isEmpty)
      assert(metadata.workload == ModuleWorkload(36, 0, 18, 18, 0, 0))
      assert(metadata.prerequisites.required.isEmpty)
      assert(
        metadata.prerequisites.recommended.contains(
          ModulePrerequisiteEntryProtocol(
            "",
            List(
              UUID.fromString("ce09539e-fc0a-4c74-b85d-40a293998bb4"),
              UUID.fromString("d4365c35-a3aa-4dab-b6a3-e17449269055"),
              UUID.fromString("84223dc5-69b9-4dbb-a6ea-45bf5e9672e3")
            ),
            Nil
          )
        )
      )
      assert(metadata.status == "active")
      assert(metadata.location == "gm")
      assert(metadata.po.mandatory.isEmpty)
      assert(
        metadata.po.optional == List(
          ModulePOOptionalProtocol(
            "wi1",
            None,
            UUID.fromString("d1cecfbc-a314-42f6-99b3-be92f22c3295"),
            partOfCatalog = false,
            List(3)
          )
        )
      )
      assert(
        metadata.participants.value == ModuleParticipants(4, 20)
      )
      assert(metadata.competences == List("analyze-domains", "model-systems"))
      assert(
        metadata.globalCriteria == List("internationalization", "digitization")
      )
      assert(metadata.taughtWith.isEmpty)
      assert(deContent.learningOutcome == "")
      assert(enContent.learningOutcome == "")
      assert(deContent.content == "")
      assert(enContent.content == "")
      assert(deContent.teachingAndLearningMethods == "")
      assert(enContent.teachingAndLearningMethods == "")
      assert(deContent.recommendedReading == "")
      assert(enContent.recommendedReading == "")
      assert(deContent.particularities == "")
      assert(enContent.particularities == "")
    }
  }
}
