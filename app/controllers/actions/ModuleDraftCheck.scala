package controllers.actions

import auth.UserTokenRequest
import models.User
import play.api.mvc.{ActionFilter, Result}
import service.{ModuleDraftService, ModuleUpdatePermissionService}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait ModuleDraftCheck { self: PermissionCheck =>
  implicit def moduleDraftService: ModuleDraftService
  implicit def moduleUpdatePermissionService: ModuleUpdatePermissionService

  def hasPermissionToEditDraft(moduleId: UUID) =
    new ActionFilter[UserTokenRequest] {
      override protected def filter[A](
          request: UserTokenRequest[A]
      ): Future[Option[Result]] = {
        val user = User(request.token.username)
        val hasPermission = for {
          b1 <- moduleUpdatePermissionService.hasPermission(user, moduleId)
          b2 <- moduleDraftService.getByModule(moduleId)
        } yield b1 || b2.user == user
        toResult(hasPermission, request)
      }

      override protected def executionContext: ExecutionContext = ctx
    }
}
