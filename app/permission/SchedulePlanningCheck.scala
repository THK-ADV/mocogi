package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import controllers.UsesClientErrors
import play.api.mvc.ActionFilter
import play.api.mvc.Result

trait SchedulePlanningCheck extends UsesClientErrors {
  protected implicit def ctx: ExecutionContext

  def hasSchedulePlanningPermission =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] =
        if request.permissions.hasAnyPermission(PermissionType.SchedulePlanning) then Future.successful(None)
        else
          Future.successful(
            Some(
              clientErrors.forbidden(request, "insufficient schedule planning permissions")
            )
          )
      protected override def executionContext: ExecutionContext = ctx
    }
}
