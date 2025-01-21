package controllers.actions

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.TokenRequest
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import service.ModuleUpdatePermissionService

trait ModuleUpdatePermissionCheck { self: PermissionCheck =>
  implicit def moduleUpdatePermissionService: ModuleUpdatePermissionService

  def hasInheritedPermission(moduleId: UUID) =
    new ActionFilter[TokenRequest] {
      protected override def filter[A](request: TokenRequest[A]): Future[Option[Result]] =
        continueAsAdmin(
          request,
          otherwise = moduleUpdatePermissionService.hasInheritedPermission(request.campusId, moduleId)
        )

      protected override def executionContext: ExecutionContext = ctx
    }
}
