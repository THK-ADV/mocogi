package database

import models.ModuleDraftStatus
import play.api.libs.json.{JsValue, Json}
import service.Print
import slick.jdbc.PostgresProfile.api._

package object table {
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

  implicit val printColumnType: BaseColumnType[Print] =
    MappedColumnType
      .base[Print, String](_.value, Print.apply)

  implicit val jsValueColumnType: BaseColumnType[JsValue] =
    MappedColumnType
      .base[JsValue, String](Json.stringify, Json.parse)
}
