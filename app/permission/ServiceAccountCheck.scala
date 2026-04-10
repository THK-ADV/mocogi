package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.TokenRequest
import controllers.UsesClientErrors
import play.api.mvc.ActionFilter
import play.api.mvc.Result

trait ServiceAccountCheck extends UsesClientErrors {
  protected implicit def ctx: ExecutionContext

  def hasRole(role: Role) =
    new ActionFilter[TokenRequest] {
      protected override def filter[A](request: TokenRequest[A]): Future[Option[Result]] =
        if request.token.roles.contains(role.id) then Future.successful(None)
        else
          Future.successful(
            Some(clientErrors.forbidden(request, "insufficient permissions"))
          )

      protected override def executionContext: ExecutionContext = ctx
    }
}
