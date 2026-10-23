package service.artifact.modulecatalog

import java.util.UUID

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

final class ModuleCatalogConfigSpec extends AnyWordSpec with Matchers {

  private val moduleId = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val optionId = UUID.fromString("00000000-0000-0000-0000-000000000002")

  "ModuleCatalogConfig JSON reads" should {
    "default a missing moduleSelection and a null studyPlan to empty values without rendering the study plan" in {
      Json.parse("""{ "studyPlan": null }""").as[ModuleCatalogConfig] shouldBe
        ModuleCatalogConfig.empty.copy(renderStudyPlan = false)
    }

    "read the nested config shape" in {
      val config = Json
        .parse(
          s"""{
             |  "moduleSelection": {
             |    "excludedModuleIds": ["$moduleId"],
             |    "excludedElectiveOptions": [
             |      {
             |        "genericModuleId": "$moduleId",
             |        "optionModuleId": "$optionId"
             |      }
             |    ]
             |  },
             |  "studyPlan": {
             |    "sections": [
             |      { "untilSemester": 3, "headline": "Grundstudium" }
             |    ],
             |    "semesterSelections": [
             |      { "moduleId": "$moduleId", "selectedSemester": 5 }
             |    ],
             |    "genericModuleOccurrences": [
             |      { "moduleId": "$moduleId", "semester": 5, "count": 2 }
             |    ]
             |  }
             |}""".stripMargin
        )
        .as[ModuleCatalogConfig]

      config.moduleSelection.excludedModuleIds shouldBe List(moduleId)
      config.moduleSelection.excludedElectiveOptions shouldBe List(
        ModuleCatalogExcludedElectiveOption(moduleId, optionId)
      )
      config.renderStudyPlan shouldBe true
      config.studyPlan.sections shouldBe List(StudyPlanSection(3, "Grundstudium"))
      config.studyPlan.semesterSelections shouldBe List(ModuleCatalogSemesterSelection(moduleId, 5))
      config.studyPlan.genericModuleOccurrences shouldBe List(ModuleCatalogGenericModuleOccurrence(moduleId, 5, 2))
    }
  }
}
