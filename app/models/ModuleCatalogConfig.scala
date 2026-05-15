package models

import java.util.UUID

import play.api.libs.json.Json
import play.api.libs.json.Reads
import printing.latex.studyplan.StudyPlanSection

final case class ModuleCatalogConfig(bannedGenericModules: List[UUID], sections: List[StudyPlanSection])

object ModuleCatalogConfig {
  given Reads[ModuleCatalogConfig] = Json.reads[ModuleCatalogConfig]
}
