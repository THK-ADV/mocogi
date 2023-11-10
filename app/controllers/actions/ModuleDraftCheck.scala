package controllers.actions

import controllers.actions.PersonAction.PersonRequest
import play.api.mvc.{ActionFilter, Result}
import service.{ModuleDraftService, ModuleUpdatePermissionService}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait ModuleDraftCheck { self: PermissionCheck =>
  implicit def moduleDraftService: ModuleDraftService
  implicit def moduleUpdatePermissionService: ModuleUpdatePermissionService

  def hasPermissionToEditDraft(moduleId: UUID) =
    new ActionFilter[PersonRequest] {
      override protected def filter[A](
          request: PersonRequest[A]
      ): Future[Option[Result]] = {
        val person = request.person
        val user = request.request.campusId
        val hasPermission = for {
          b1 <- moduleUpdatePermissionService.hasPermission(user, moduleId)
          b2 <- moduleDraftService.getByModuleOpt(moduleId)
        } yield b1 || b2.exists(a => person.username.contains(a.author))
        toResult(hasPermission, request.request)
      }

      override protected def executionContext: ExecutionContext = ctx
    }
}
