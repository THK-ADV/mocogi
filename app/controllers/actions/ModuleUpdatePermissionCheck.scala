package controllers.actions

import auth.UserTokenRequest
import play.api.mvc.{ActionFilter, Result}
import service.ModuleUpdatePermissionService

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait ModuleUpdatePermissionCheck { self: PermissionCheck =>
  implicit def moduleUpdatePermissionService: ModuleUpdatePermissionService

  def hasPermissionToGrantPermission(moduleId: UUID) =
    new ActionFilter[UserTokenRequest] {
      override protected def filter[A](
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

      override protected def executionContext: ExecutionContext = ctx
    }
}
