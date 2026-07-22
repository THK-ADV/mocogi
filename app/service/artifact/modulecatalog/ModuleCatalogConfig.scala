package service.artifact.modulecatalog

import java.util.UUID

import play.api.libs.functional.syntax.*
import play.api.libs.json.JsPath
import play.api.libs.json.Json
import play.api.libs.json.Reads
import service.artifact.*

final case class ModuleCatalogConfig(
    moduleSelection: ModuleCatalogModuleSelectionConfig,
    studyPlan: ModuleCatalogStudyPlanConfig
) {
  def bannedGenericModules: List[UUID] = moduleSelection.excludedModuleIds

  def sections: List[StudyPlanSection] = studyPlan.sections
}

final case class ModuleCatalogModuleSelectionConfig(
    excludedModuleIds: List[UUID],
    excludedElectiveOptions: List[ModuleCatalogExcludedElectiveOption]
)

final case class ModuleCatalogExcludedElectiveOption(genericModuleId: UUID, optionModuleId: UUID)

final case class ModuleCatalogStudyPlanConfig(
    sections: List[StudyPlanSection],
    semesterSelections: List[ModuleCatalogSemesterSelection],
    genericModuleOccurrences: List[ModuleCatalogGenericModuleOccurrence]
)

final case class StudyPlanSection(untilSemester: Int, headline: String)

final case class ModuleCatalogSemesterSelection(moduleId: UUID, selectedSemester: Int)

final case class ModuleCatalogGenericModuleOccurrence(moduleId: UUID, semester: Int, count: Int)

object ModuleCatalogConfig {
  def empty: ModuleCatalogConfig = ModuleCatalogConfig(
    ModuleCatalogModuleSelectionConfig.empty,
    ModuleCatalogStudyPlanConfig.empty
  )

  given Reads[ModuleCatalogConfig] =
    (JsPath \ "moduleSelection")
      .readNullable[ModuleCatalogModuleSelectionConfig]
      .map(_.getOrElse(ModuleCatalogModuleSelectionConfig.empty))
      .and(
        (JsPath \ "studyPlan")
          .readNullable[ModuleCatalogStudyPlanConfig]
          .map(_.getOrElse(ModuleCatalogStudyPlanConfig.empty))
      )
      .and((JsPath \ "bannedGenericModules").readNullable[List[UUID]].map(_.getOrElse(Nil)))
      .and((JsPath \ "sections").readNullable[List[StudyPlanSection]].map(_.getOrElse(Nil))) {
        (moduleSelection, studyPlan, bannedGenericModules, sections) =>
          val effectiveModuleSelection =
            if bannedGenericModules.isEmpty then moduleSelection
            else
              moduleSelection
                .copy(excludedModuleIds = (moduleSelection.excludedModuleIds ++ bannedGenericModules).distinct)
          val effectiveStudyPlan =
            if studyPlan.sections.nonEmpty || sections.isEmpty then studyPlan
            else studyPlan.copy(sections = sections)

          ModuleCatalogConfig(effectiveModuleSelection, effectiveStudyPlan)
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
      .readNullable[List[StudyPlanSection]]
      .map(_.getOrElse(Nil))
      .and((JsPath \ "semesterSelections").readNullable[List[ModuleCatalogSemesterSelection]].map(_.getOrElse(Nil)))
      .and(
        (JsPath \ "genericModuleOccurrences")
          .readNullable[List[ModuleCatalogGenericModuleOccurrence]]
          .map(_.getOrElse(Nil))
      )(ModuleCatalogStudyPlanConfig.apply)
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
