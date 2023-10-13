package controllers.actions

import auth.UserTokenRequest
import models.User
import play.api.mvc.{ActionFilter, Result}
import service.ModuleUpdatePermissionService

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait ModuleUpdatePermissionCheck { self: PermissionCheck =>
  implicit def moduleUpdatePermissionService: ModuleUpdatePermissionService

  def hasPermissionForModule(moduleId: UUID) =
    new ActionFilter[UserTokenRequest] {
      override protected def filter[A](
          request: UserTokenRequest[A]
      ): Future[Option[Result]] =
        toResult(
          moduleUpdatePermissionService
            .hasPermission(
              User(request.token.username),
              moduleId
            ),
          request
        )

      override protected def executionContext: ExecutionContext = ctx
    }
}
