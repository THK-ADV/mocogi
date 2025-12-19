package database

import auth.CampusId
import git.Branch
import git.CommitId
import git.MergeRequestId
import git.MergeRequestStatus
import models.*
import service.pipeline.Print
import slick.jdbc.PostgresProfile.api.*

package object table {
  implicit val moduleRelationColumnType: BaseColumnType[ModuleRelationType] =
    MappedColumnType
      .base[ModuleRelationType, String](_.id, ModuleRelationType.apply)

  implicit val responsibilityTypeColumnType: BaseColumnType[ResponsibilityType] =
    MappedColumnType
      .base[ResponsibilityType, String](_.id, ResponsibilityType.apply)

  implicit val prerequisiteTypeColumnType: BaseColumnType[PrerequisiteType] =
    MappedColumnType
      .base[PrerequisiteType, String](
        _.id,
        PrerequisiteType.apply
      )

  implicit val moduleDraftStatusColumnType: BaseColumnType[ModuleDraftSource] =
    MappedColumnType
      .base[ModuleDraftSource, String](_.id, ModuleDraftSource.apply)

  implicit val printColumnType: BaseColumnType[Print] =
    MappedColumnType
      .base[Print, String](_.value, Print.apply)

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

  implicit val universityRoleColumnType: BaseColumnType[UniversityRole] =
    MappedColumnType
      .base[UniversityRole, String](_.id, UniversityRole.apply)

  implicit val moduleReviewStatusColumnType: BaseColumnType[ModuleReviewStatus] =
    MappedColumnType
      .base[ModuleReviewStatus, String](_.id, ModuleReviewStatus.apply)

  implicit val moduleUpdatePermissionTypeColumnType: BaseColumnType[ModuleUpdatePermissionType] =
    MappedColumnType
      .base[ModuleUpdatePermissionType, String](
        _.id,
        ModuleUpdatePermissionType.apply
      )

  implicit val mergeRequestStatusColumnType: BaseColumnType[MergeRequestStatus] =
    MappedColumnType
      .base[MergeRequestStatus, String](
        _.id,
        MergeRequestStatus.apply
      )

  given BaseColumnType[AssessmentMethodSource] =
    MappedColumnType.base[AssessmentMethodSource, String](
      _.id,
      {
        case "unknown" => AssessmentMethodSource.Unknown
        case "rpo"     => AssessmentMethodSource.RPO
        case other     => AssessmentMethodSource.PO(FullPoId(other))
      }
    )
}
