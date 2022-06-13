package parsing

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ModuleCompendiumParser.moduleCompendiumParser
import parsing.types._

import java.util.UUID

class ModuleCompendiumParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Module Compendium Parser" should {
    "parse a module-compendium1.duda" in {
      val (res, rest) =
        withTestFile("module-compendium1.duda")(moduleCompendiumParser.parse)
      assert(rest.isEmpty)
      val (metadata, deContent, enContent) =
        ModuleCompendium.unapply(res.value).get
      assert(
        metadata.id == UUID.fromString("00895144-30e4-4bd2-b800-bb706686d950")
      )
      assert(metadata.title == "Module A")
      assert(metadata.abbrev == "MA")
      assert(metadata.kind == ModuleType("mandatory", "Pflicht"))
      assert(metadata.credits == 5)
      assert(metadata.language == Language("de", "Deutsch"))
      assert(metadata.duration == 1)
      assert(metadata.recommendedSemester == 3)
      assert(metadata.frequency == Season("ws", "Wintersemester"))
      assert(
        metadata.responsibilities == Responsibilities(
          List(
            People("ald", "Dobrynin", "Alexander", "M.Sc.", "F10")
          ),
          List(
            People("ald", "Dobrynin", "Alexander", "M.Sc.", "F10"),
            People("abe", "Bertels", "Anja", "B.Sc.", "F10")
          )
        )
      )
      assert(
        metadata.assessmentMethod == List(
          AssessmentMethod("written-exam", "Klausurarbeiten")
        )
      )
      assert(metadata.workload == Workload(150, 36, 0, 18, 18, 78))
      assert(metadata.recommendedPrerequisites == List("ap1", "ap2", "ma1"))
      assert(metadata.requiredPrerequisites == List.empty)
      assert(metadata.status == Status("active", "Aktiv"))
      assert(metadata.location == Location("gm", "Gummersbach"))
      assert(metadata.po == List("AI2"))
      assert(deContent.recommendedPrerequisitesBody == "\nProgrammieren\n\n")
      assert(enContent.recommendedPrerequisitesBody == "\nProgramming\n\n")
      assert(deContent.learningOutcomeBody == "\nProgrammieren lernen\n\n")
      assert(enContent.learningOutcomeBody == "\nLearn to code\n\n")
      assert(deContent.contentBody == "\n- Klassen\n- Vererbung\n- Polymorphie\n\n")
      assert(
        enContent.contentBody == "\n- Classes\n- Inheritance\n- Polymorphism\n\n"
      )
      assert(deContent.teachingAndLearningMethodsBody == "\nSlides, Whiteboard\n\n")
      assert(enContent.teachingAndLearningMethodsBody == "\n")
      assert(
        deContent.recommendedReadingBody == "\nProgrammieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt\n\n"
      )
      assert(
        enContent.recommendedReadingBody == "\nProgrammieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt\n\n"
      )
      assert(deContent.particularitiesBody == "\nnichts\n\n")
      assert(enContent.particularitiesBody == "\nnothing")
    }
  }
}
