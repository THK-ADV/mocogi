package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import play.api.libs.json.Json
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden

trait ExamPlanningCheck {
  protected implicit def ctx: ExecutionContext

  def canUpdateExamPlan =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] =
        if request.permissions.hasAnyPermission(PermissionType.ExamPlanning) then Future.successful(None)
        else Future.successful(Some(Forbidden(Json.obj("message" -> "insufficient exam planning permissions"))))
      protected override def executionContext: ExecutionContext = ctx
    }
}
