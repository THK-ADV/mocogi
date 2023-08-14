package auth

import models.User
import play.api.libs.json.Json
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionFilter, Result}
import service.ModuleUpdatePermissionService

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait ModuleUpdatePermissionCheck {
  implicit def moduleUpdatePermissionService: ModuleUpdatePermissionService
  implicit def ctx: ExecutionContext

  def hasPermissionForModule(moduleId: UUID) =
    new ActionFilter[UserTokenRequest] {
      override protected def filter[A](
          request: UserTokenRequest[A]
      ): Future[Option[Result]] = moduleUpdatePermissionService
        .hasPermission(
          User(request.token.username),
          moduleId
        )
        .map(hasPermission =>
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

      override protected def executionContext: ExecutionContext = ctx
    }
}
