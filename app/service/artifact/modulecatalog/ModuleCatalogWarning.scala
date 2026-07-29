package service.artifact.modulecatalog

import java.util.UUID

final case class ModuleCatalogWarning(code: String, message: String, moduleId: Option[UUID])
