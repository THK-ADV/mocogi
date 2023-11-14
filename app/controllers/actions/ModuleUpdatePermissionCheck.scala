package controllers.actions

import auth.UserTokenRequest
import controllers.ModuleUpdatePermissionController._
import play.api.mvc.{ActionFilter, Result}
import service.ModuleUpdatePermissionService

import scala.concurrent.{ExecutionContext, Future}

trait ModuleUpdatePermissionCheck { self: PermissionCheck =>
  implicit def moduleUpdatePermissionService: ModuleUpdatePermissionService

  def hasPermissionToGrantPermission =
    new ActionFilter[UserTokenRequest] {
      override protected def filter[A](
          request: UserTokenRequest[A]
      ): Future[Option[Result]] = {
        if (request.campusId.value == "adobryni")
          Future.successful(None) // TODO DEBUG ONLY
        else
          request.body match {
            case json: ModuleUpdatePermissionProtocol =>
              toResult(
                moduleUpdatePermissionService
                  .hasInheritedPermission(request.campusId, json.module),
                request
              )
            case _ => Future.failed(new Throwable("expected module id in json"))
          }
      }

      override protected def executionContext: ExecutionContext = ctx
    }
}
