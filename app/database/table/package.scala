package database

import models.ModuleDraftStatus
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

  implicit val assessmentMethodTypeColumnType
      : BaseColumnType[AssessmentMethodType] =
    MappedColumnType
      .base[AssessmentMethodType, String](
        _.toString,
        AssessmentMethodType.apply
      )

  implicit val prerequisiteTypeColumnType: BaseColumnType[PrerequisiteType] =
    MappedColumnType
      .base[PrerequisiteType, String](
        _.toString,
        PrerequisiteType.apply
      )

  implicit val listIntColumnType: BaseColumnType[List[Int]] =
    MappedColumnType
      .base[List[Int], String](
        xs => if (xs.isEmpty) "" else xs.mkString(","),
        s =>
          if (s.isEmpty) Nil
          else
            s.split(",").foldLeft(List.empty[Int]) { case (acc, s) =>
              s.toInt :: acc
            }
      )

  implicit val moduleDraftStatusColumnType: BaseColumnType[ModuleDraftStatus] =
    MappedColumnType
      .base[ModuleDraftStatus, String](_.toString, ModuleDraftStatus.apply)
}
