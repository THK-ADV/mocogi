package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.Role
import auth.TokenRequest
import play.api.libs.json.Json
import play.api.mvc.Results.Forbidden

@deprecated
trait PermissionCheck {
  implicit def ctx: ExecutionContext

  def continueAsAdmin[A](request: TokenRequest[A], otherwise: => Future[Boolean]) =
    if request.token.hasRole(Role.Admin) then Future.successful(None) else toResult(otherwise, request)

  def toResult[A](f: Future[Boolean], request: TokenRequest[A]) =
    f.map(hasPermission =>
      if (hasPermission) None
      else
        Some(
          Forbidden(
            Json.obj(
              "request" -> request.toString(),
              "message" -> s"user ${request.token.username} has insufficient permissions for the given request"
            )
          )
        )
    )
}
