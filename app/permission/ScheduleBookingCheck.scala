package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import play.api.libs.json.Json
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden

trait ScheduleBookingCheck {
  protected implicit def ctx: ExecutionContext

  def canUpdateBookings = new ActionFilter[UserRequest] {
    protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] =
      if request.permissions.hasScheduleBooking then Future.successful(None)
      else Future.successful(Some(Forbidden(Json.obj("message" -> "insufficient schedule booking permissions"))))
    protected override def executionContext: ExecutionContext = ctx
  }
}
