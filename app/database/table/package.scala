package database

import auth.CampusId
import git.{Branch, CommitId, MergeRequestId, MergeRequestStatus}
import models._
import play.api.libs.json.{JsValue, Json}
import service.Print
import slick.jdbc.PostgresProfile.api._

package object table {
  implicit val moduleRelationColumnType: BaseColumnType[ModuleRelationType] =
    MappedColumnType
      .base[ModuleRelationType, String](_.id, ModuleRelationType.apply)

  implicit val responsibilityTypeColumnType
      : BaseColumnType[ResponsibilityType] =
    MappedColumnType
      .base[ResponsibilityType, String](_.id, ResponsibilityType.apply)

  implicit val assessmentMethodTypeColumnType
      : BaseColumnType[AssessmentMethodType] =
    MappedColumnType
      .base[AssessmentMethodType, String](
        _.id,
        AssessmentMethodType.apply
      )

  implicit val prerequisiteTypeColumnType: BaseColumnType[PrerequisiteType] =
    MappedColumnType
      .base[PrerequisiteType, String](
        _.id,
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

  implicit val moduleDraftStatusColumnType: BaseColumnType[ModuleDraftSource] =
    MappedColumnType
      .base[ModuleDraftSource, String](_.id, ModuleDraftSource.apply)

  implicit val printColumnType: BaseColumnType[Print] =
    MappedColumnType
      .base[Print, String](_.value, Print.apply)

  implicit val jsValueColumnType: BaseColumnType[JsValue] =
    MappedColumnType
      .base[JsValue, String](Json.stringify, Json.parse)

  implicit val campusIdColumnType: BaseColumnType[CampusId] =
    MappedColumnType
      .base[CampusId, String](_.value, CampusId.apply)

  implicit val branchColumnType: BaseColumnType[Branch] =
    MappedColumnType
      .base[Branch, String](_.value, Branch.apply)

  implicit val commitColumnType: BaseColumnType[CommitId] =
    MappedColumnType
      .base[CommitId, String](_.value, CommitId.apply)

  implicit val mergeRequestIdColumnType: BaseColumnType[MergeRequestId] =
    MappedColumnType
      .base[MergeRequestId, Int](_.value, MergeRequestId.apply)

  def iterableToString(xs: Iterable[String]): String =
    if (xs.isEmpty) "" else xs.mkString(",")

  def stringToList(s: String): List[String] =
    if (s.isEmpty) Nil
    else
      s.split(",").foldLeft(List.empty[String]) { case (acc, s) =>
        s :: acc
      }

  def stringToSet(s: String): Set[String] =
    if (s.isEmpty) Set.empty
    else
      s.split(",").foldLeft(Set.empty[String]) { case (acc, s) =>
        acc.+(s)
      }

  implicit val listStringColumnType: BaseColumnType[List[String]] =
    MappedColumnType
      .base[List[String], String](
        iterableToString,
        stringToList
      )

  implicit val setStringColumnType: BaseColumnType[Set[String]] =
    MappedColumnType
      .base[Set[String], String](
        iterableToString,
        stringToSet
      )

  implicit val universityRoleColumnType: BaseColumnType[UniversityRole] =
    MappedColumnType
      .base[UniversityRole, String](_.id, UniversityRole.apply)

  implicit val moduleReviewStatusColumnType
      : BaseColumnType[ModuleReviewStatus] =
    MappedColumnType
      .base[ModuleReviewStatus, String](_.id, ModuleReviewStatus.apply)

  implicit val moduleUpdatePermissionTypeColumnType
      : BaseColumnType[ModuleUpdatePermissionType] =
    MappedColumnType
      .base[ModuleUpdatePermissionType, String](
        _.id,
        ModuleUpdatePermissionType.apply
      )

  implicit val mergeRequestStatusColumnType
      : BaseColumnType[MergeRequestStatus] =
    MappedColumnType
      .base[MergeRequestStatus, String](
        _.id,
        MergeRequestStatus.apply
      )
}
