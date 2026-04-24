package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import controllers.UsesClientErrors
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import service.StudyProgramPrivilegesService

trait ArtifactCheck extends UsesClientErrors {
  protected def studyProgramPrivilegesService: StudyProgramPrivilegesService
  protected implicit def ctx: ExecutionContext

  /**
   * This method checks if the user can create artifacts for the given study program.
   */
  def canCreateArtifact(studyProgram: String) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        if request.permissions.isAdmin then Future.successful(None)
        else {
          studyProgramPrivilegesService
            .studyProgramIdsForPOs(request.permissions.artifactsCreatePermissions)
            .map { studyPrograms =>
              if studyPrograms.contains(studyProgram) then None
              else
                Some(
                  forbiddenForUser(
                    request,
                    request.request.token.username,
                    Some(s"to create artifacts for $studyProgram")
                  )
                )
            }
        }
      }

      protected override def executionContext: ExecutionContext = ctx
    }

  /**
   * This method checks if the user can preview artifacts for the given study program.
   */
  def canPreviewArtifact(studyProgram: String) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        if request.permissions.isAdmin then Future.successful(None)
        else {
          studyProgramPrivilegesService
            .studyProgramIdsForPOs(request.permissions.artifactsPreviewPermissions)
            .map { studyPrograms =>
              if studyPrograms.contains(studyProgram) then None
              else
                Some(
                  forbiddenForUser(
                    request,
                    request.request.token.username,
                    Some(s"to preview artifacts for $studyProgram")
                  )
                )
            }
        }
      }

      protected override def executionContext: ExecutionContext = ctx
    }
}
