package service.artifact.modulecatalog

import java.util.UUID

import play.api.libs.json.Json
import play.api.libs.json.Writes

final case class ModuleCatalogWarning(code: String, message: String, moduleId: Option[UUID])

object ModuleCatalogWarning {
  implicit def writes: Writes[ModuleCatalogWarning] = Json.writes
}
