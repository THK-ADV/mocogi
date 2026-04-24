package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import controllers.UsesClientErrors
import play.api.mvc.ActionFilter
import play.api.mvc.Result

trait AdminCheck extends UsesClientErrors {
  protected implicit def ctx: ExecutionContext

  def isAdmin =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] =
        if request.permissions.isAdmin then Future.successful(None)
        else
          Future.successful(
            Some(
              forbiddenForUser(request, request.request.token.username)
            )
          )

      protected override def executionContext: ExecutionContext = ctx
    }
}
