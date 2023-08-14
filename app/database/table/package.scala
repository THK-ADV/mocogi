package database

import models._
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

  def intsToString(xs: List[Int]): String =
    if (xs.isEmpty) "" else xs.mkString(",")

  def stringToInts(s: String): List[Int] =
    if (s.isEmpty) Nil
    else
      s.split(",").foldLeft(List.empty[Int]) { case (acc, s) =>
        s.toInt :: acc
      }

  implicit val listIntColumnType: BaseColumnType[List[Int]] =
    MappedColumnType
      .base[List[Int], String](
        intsToString,
        stringToInts
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

  implicit val userColumnType: BaseColumnType[User] =
    MappedColumnType
      .base[User, String](_.username, User.apply)

  implicit val branchColumnType: BaseColumnType[Branch] =
    MappedColumnType
      .base[Branch, String](_.value, Branch.apply)

  implicit val commitColumnType: BaseColumnType[CommitId] =
    MappedColumnType
      .base[CommitId, String](_.value, CommitId.apply)

  implicit val mergeRequestColumnType: BaseColumnType[MergeRequestId] =
    MappedColumnType
      .base[MergeRequestId, Int](_.value, MergeRequestId.apply)

  def listToString(xs: List[String]): String =
    if (xs.isEmpty) "" else xs.mkString(",")

  def stringToList(s: String): List[String] =
    if (s.isEmpty) Nil
    else
      s.split(",").foldLeft(List.empty[String]) { case (acc, s) =>
        s :: acc
      }

  implicit val listStringColumnType: BaseColumnType[List[String]] =
    MappedColumnType
      .base[List[String], String](
        listToString,
        stringToList
      )
}
