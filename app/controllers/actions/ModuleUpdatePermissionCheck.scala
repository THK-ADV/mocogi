package controllers.actions

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.UserTokenRequest
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import service.ModuleUpdatePermissionService

trait ModuleUpdatePermissionCheck { self: PermissionCheck =>
  implicit def moduleUpdatePermissionService: ModuleUpdatePermissionService

  def hasInheritedPermission(moduleId: UUID) =
    new ActionFilter[UserTokenRequest] {
      protected override def filter[A](
          request: UserTokenRequest[A]
      ): Future[Option[Result]] = {
        if (request.campusId.value == "adobryni")
          Future.successful(None) // TODO DEBUG ONLY
        else
          toResult(
            moduleUpdatePermissionService
              .hasInheritedPermission(request.campusId, moduleId),
            request
          )
      }

      protected override def executionContext: ExecutionContext = ctx
    }
}
