package service.artifact.modulecatalog

import java.util.UUID

import play.api.libs.functional.syntax.*
import play.api.libs.json.JsDefined
import play.api.libs.json.JsError
import play.api.libs.json.JsNull
import play.api.libs.json.JsObject
import play.api.libs.json.JsPath
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.libs.json.Reads
import service.artifact.*

final case class ModuleCatalogConfig(
    moduleSelection: ModuleCatalogModuleSelectionConfig,
    studyPlan: ModuleCatalogStudyPlanConfig,
    renderStudyPlan: Boolean = true
)

final case class ModuleCatalogModuleSelectionConfig(
    excludedModuleIds: List[UUID],
    excludedElectiveOptions: List[ModuleCatalogExcludedElectiveOption]
)

final case class ModuleCatalogExcludedElectiveOption(genericModuleId: UUID, optionModuleId: UUID)

final case class ModuleCatalogStudyPlanConfig(
    sections: List[StudyPlanSection],
    semesterSelections: List[ModuleCatalogSemesterSelection],
    genericModuleOccurrences: List[ModuleCatalogGenericModuleOccurrence],
    alternativeGenericModuleOccurrences: List[ModuleCatalogGenericModuleOccurrence] = Nil,
    alternativeModuleDistributions: List[ModuleCatalogModuleDistribution] = Nil
)

final case class StudyPlanSection(untilSemester: Int, headline: String)

final case class ModuleCatalogSemesterSelection(moduleId: UUID, selectedSemester: Int)

final case class ModuleCatalogGenericModuleOccurrence(moduleId: UUID, semester: Int, count: Int)

final case class ModuleCatalogModuleDistribution(moduleId: UUID, semesters: List[Int])

object ModuleCatalogConfig {
  def empty: ModuleCatalogConfig = ModuleCatalogConfig(
    ModuleCatalogModuleSelectionConfig.empty,
    ModuleCatalogStudyPlanConfig.empty
  )

  given Reads[ModuleCatalogConfig] = Reads { json =>
    for {
      moduleSelection <- (JsPath \ "moduleSelection")
        .readNullable[ModuleCatalogModuleSelectionConfig]
        .reads(json)
      studyPlan <- (json \ "studyPlan") match {
        case JsDefined(JsNull)          => JsSuccess(None)
        case JsDefined(value: JsObject) => value.validate[ModuleCatalogStudyPlanConfig].map(Some(_))
        case JsDefined(_)               => JsError("studyPlan must be null or an object")
        case _                          => JsError("studyPlan is required")
      }
    } yield ModuleCatalogConfig(
      moduleSelection.getOrElse(ModuleCatalogModuleSelectionConfig.empty),
      studyPlan.getOrElse(ModuleCatalogStudyPlanConfig.empty),
      studyPlan.isDefined
    )
  }
}

object ModuleCatalogModuleSelectionConfig {
  def empty: ModuleCatalogModuleSelectionConfig = ModuleCatalogModuleSelectionConfig(Nil, Nil)

  given Reads[ModuleCatalogModuleSelectionConfig] =
    (JsPath \ "excludedModuleIds")
      .readNullable[List[UUID]]
      .map(_.getOrElse(Nil))
      .and(
        (JsPath \ "excludedElectiveOptions")
          .readNullable[List[ModuleCatalogExcludedElectiveOption]]
          .map(_.getOrElse(Nil))
      )(ModuleCatalogModuleSelectionConfig.apply)
}

object ModuleCatalogExcludedElectiveOption {
  given Reads[ModuleCatalogExcludedElectiveOption] = Json.reads
}

object ModuleCatalogStudyPlanConfig {
  def empty: ModuleCatalogStudyPlanConfig = ModuleCatalogStudyPlanConfig(Nil, Nil, Nil)

  given Reads[ModuleCatalogStudyPlanConfig] =
    (JsPath \ "sections")
      .read[List[StudyPlanSection]]
      .and((JsPath \ "semesterSelections").read[List[ModuleCatalogSemesterSelection]])
      .and((JsPath \ "genericModuleOccurrences").read[List[ModuleCatalogGenericModuleOccurrence]])
      .and(
        (JsPath \ "alternative" \ "genericModuleOccurrences")
          .readNullable[List[ModuleCatalogGenericModuleOccurrence]]
          .map(_.getOrElse(Nil))
      )
      .and(
        (JsPath \ "alternative" \ "moduleDistributions")
          .readNullable[List[ModuleCatalogModuleDistribution]]
          .map(_.getOrElse(Nil))
      )(ModuleCatalogStudyPlanConfig(_, _, _, _, _))
}

object StudyPlanSection {
  given Reads[StudyPlanSection] = Json.reads[StudyPlanSection]
}

object ModuleCatalogSemesterSelection {
  given Reads[ModuleCatalogSemesterSelection] = Json.reads
}

object ModuleCatalogGenericModuleOccurrence {
  given Reads[ModuleCatalogGenericModuleOccurrence] = Json.reads
}

object ModuleCatalogModuleDistribution {
  given Reads[ModuleCatalogModuleDistribution] = Json.reads
}
