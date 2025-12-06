package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.TokenRequest
import play.api.libs.json.Json
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden

trait ServiceAccountCheck {
  protected implicit def ctx: ExecutionContext

  def hasRole(role: Role) =
    new ActionFilter[TokenRequest] {
      protected override def filter[A](request: TokenRequest[A]): Future[Option[Result]] =
        if request.token.roles.contains(role.id) then Future.successful(None)
        else
          Future.successful(
            Some(Forbidden(Json.obj("request" -> request.toString(), "message" -> "insufficient permissions")))
          )

      protected override def executionContext: ExecutionContext = ctx
    }
}
