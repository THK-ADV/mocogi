package service

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.StudyProgramPersonRepository
import models.StudyProgramPrivileges
import permission.PermissionType
import permission.PermissionType.ArtifactsCreate
import permission.PermissionType.ArtifactsPreview
import permission.Permissions

@Singleton
final class StudyProgramPrivilegesService @Inject() (
    repo: StudyProgramPersonRepository,
    implicit val ctx: ExecutionContext
) {

  def studyProgramIdsForPOs(pos: Set[String]): Future[Seq[String]] =
    repo.studyProgramIdsForPOs(pos)

  /**
   * Retrieves study program privileges for a person based on their permissions:
   * - If Admin: returns all study programs with all roles
   * - If ArtifactsPreview or ArtifactsCreate: synthesizes privileges from all study programs
   * - Otherwise: fetches person-specific privileges from DB
   */
  def getStudyProgramPrivileges(person: String, permissions: Permissions): Future[Iterable[StudyProgramPrivileges]] = {
    if permissions.isAdmin then
      repo.getAllStudyProgramsWithoutSpecialization().map(_.map(StudyProgramPrivileges(_, true, true)))
    else if permissions.hasAnyPermission(ArtifactsPreview, ArtifactsCreate) then {
      val preview = permissions.get(ArtifactsPreview).getOrElse(Set.empty)
      val create  = permissions.get(ArtifactsCreate).getOrElse(Set.empty)
      repo
        .getAllStudyProgramsWithoutSpecializationForPOs(preview ++ create)
        .map(_.map { sp =>
          // ArtifactsCreate is a superset of ArtifactsPreview (see [[PermissionRepository]]).
          val canCreate = create.contains(sp.po.id)
          StudyProgramPrivileges(sp, canCreate, true)
        })
    } else repo.getStudyProgramPrivilegesForPerson(person)
  }
}
