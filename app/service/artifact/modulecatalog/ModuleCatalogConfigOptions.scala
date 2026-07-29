package service.artifact.modulecatalog

import java.util.UUID

import play.api.libs.json.Json
import play.api.libs.json.Writes
import service.artifact.*

final case class ModuleCatalogConfigOptions(
    modules: Vector[ModuleCatalogModuleOption],
    genericElectiveGroups: Vector[ModuleCatalogGenericElectiveGroup],
    specializations: Vector[ModuleCatalogSpecializationOption]
)

final case class ModuleCatalogModuleOption(
    id: UUID,
    title: String,
    abbrev: String,
    ects: Double,
    moduleType: String,
    recommendedSemesters: List[Int],
    mandatory: Boolean,
    optional: Boolean,
    specializations: List[String],
    defaultIncluded: Boolean
)

final case class ModuleCatalogGenericElectiveGroup(
    genericModuleId: UUID,
    title: String,
    abbrev: String,
    optionCandidates: Vector[ModuleCatalogElectiveOptionCandidate]
)

final case class ModuleCatalogSpecializationOption(id: String, label: String)

final case class ModuleCatalogElectiveOptionCandidate(moduleId: UUID, title: String, abbrev: String, ects: Double)

object ModuleCatalogModuleOption {
  implicit def writes: Writes[ModuleCatalogModuleOption] = Json.writes
}

object ModuleCatalogElectiveOptionCandidate {
  implicit def writes: Writes[ModuleCatalogElectiveOptionCandidate] = Json.writes
}

object ModuleCatalogGenericElectiveGroup {
  implicit def writes: Writes[ModuleCatalogGenericElectiveGroup] = Json.writes
}

object ModuleCatalogSpecializationOption {
  implicit def writes: Writes[ModuleCatalogSpecializationOption] = Json.writes
}

object ModuleCatalogConfigOptions {
  implicit def writes: Writes[ModuleCatalogConfigOptions] = Json.writes
}
