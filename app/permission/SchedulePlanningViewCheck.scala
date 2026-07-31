package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import database.repo.PermissionRepository
import play.api.libs.json.Json
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden

trait SchedulePlanningViewCheck {
  protected def permissionRepository: PermissionRepository
  protected implicit def ctx: ExecutionContext

  def canViewPlanDraft =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        permissionRepository
          .getUserInfo(request.person.id, request.request.campusId, request.permissions)
          .map(i =>
            Option.unless(i.hasSchedulePlanningViewPrivileges)(
              Forbidden(Json.obj("message" -> "insufficient permission to view schedule planning"))
            )
          )
      }

      protected override def executionContext: ExecutionContext = ctx
    }
}
