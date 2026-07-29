package service.artifact.modulecatalog

import java.util.UUID

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

final class ModuleCatalogConfigSpec extends AnyWordSpec with Matchers {

  private val moduleId = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val optionId = UUID.fromString("00000000-0000-0000-0000-000000000002")

  "ModuleCatalogConfig JSON reads" should {
    "default missing objects and arrays to empty values" in {
      Json.parse("{}").as[ModuleCatalogConfig] shouldBe ModuleCatalogConfig.empty
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
      config.studyPlan.semesterSelections shouldBe List(ModuleCatalogSemesterSelection(moduleId, 5))
      config.studyPlan.genericModuleOccurrences shouldBe List(ModuleCatalogGenericModuleOccurrence(moduleId, 5, 2))
    }
  }
}
