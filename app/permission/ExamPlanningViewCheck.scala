package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import database.repo.PermissionRepository
import play.api.libs.json.Json
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden

trait ExamPlanningViewCheck {
  protected def permissionRepository: PermissionRepository
  protected implicit def ctx: ExecutionContext

  def canViewExamPlan =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        permissionRepository
          .getUserInfo(request.person.id, request.request.campusId, request.permissions)
          .map(i =>
            Option.unless(i.hasExamPlanningViewPrivileges)(
              Forbidden(Json.obj("message" -> "insufficient permission to view exam planning"))
            )
          )
      }

      protected override def executionContext: ExecutionContext = ctx
    }
}
