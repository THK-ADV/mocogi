package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import play.api.libs.json.Json
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden

trait AdminCheck {
  protected implicit def ctx: ExecutionContext

  def isAdmin =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] =
        if request.permissions.isAdmin then Future.successful(None)
        else
          Future.successful(
            Some(
              Forbidden(
                Json.obj(
                  "request" -> request.toString(),
                  "message" -> s"user ${request.request.token.username} has insufficient permissions"
                )
              )
            )
          )

      protected override def executionContext: ExecutionContext = ctx
    }
}
