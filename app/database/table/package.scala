package database

import slick.jdbc.PostgresProfile.api._

package object table {
  // TODO use this whenever a custom type needs to be mapped to a column type
  implicit val moduleRelationColumnType: BaseColumnType[ModuleRelationType] =
    MappedColumnType
      .base[ModuleRelationType, String](_.toString, ModuleRelationType.apply)

  implicit val responsibilityTypeColumnType
      : BaseColumnType[ResponsibilityType] =
    MappedColumnType
      .base[ResponsibilityType, String](_.toString, ResponsibilityType.apply)
}
