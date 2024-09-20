package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.UserTokenRequest
import play.api.libs.json.Json
import play.api.mvc.Results.Forbidden

trait PermissionCheck {
  implicit def ctx: ExecutionContext

  def toResult[A](f: Future[Boolean], request: UserTokenRequest[A]) =
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
