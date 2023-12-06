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
        val campusId = request.request.campusId
        val person = request.person
        val hasPermission = for {
          b1 <- moduleUpdatePermissionService.hasPermission(campusId, moduleId)
          b2 <- moduleDraftService.isAuthorOf(moduleId, person.id)
        } yield b1 || b2
        toResult(hasPermission, request.request)
      }

      override protected def executionContext: ExecutionContext = ctx
    }
}
