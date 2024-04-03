package controllers.actions

import controllers.actions.PersonAction.PersonRequest
import play.api.mvc.{ActionFilter, Result}
import service.{
  ModuleApprovalService,
  ModuleDraftService,
  ModuleUpdatePermissionService
}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait ModuleDraftCheck { self: PermissionCheck =>
  implicit def moduleDraftService: ModuleDraftService
  implicit def moduleUpdatePermissionService: ModuleUpdatePermissionService

  private def hasPermissionToEditDraft0[A](
      moduleId: UUID,
      request: PersonRequest[A]
  ): Future[Boolean] = {
    val campusId = request.request.campusId
    val person = request.person
    for {
      b1 <- moduleUpdatePermissionService.hasPermission(campusId, moduleId)
      b2 <- moduleDraftService.isAuthorOf(moduleId, person.id)
    } yield b1 || b2
  }

  def hasPermissionToEditDraft(moduleId: UUID) =
    new ActionFilter[PersonRequest] {
      override protected def filter[A](
          request: PersonRequest[A]
      ): Future[Option[Result]] =
        toResult(hasPermissionToEditDraft0(moduleId, request), request.request)

      override protected def executionContext: ExecutionContext = ctx
    }

  def hasPermissionToViewDraft(
      moduleId: UUID,
      moduleApprovalService: ModuleApprovalService
  ) =
    new ActionFilter[PersonRequest] {
      override protected def filter[A](
          request: PersonRequest[A]
      ): Future[Option[Result]] = {
        val person = request.person
        val hasPermission = for {
          b1 <- hasPermissionToEditDraft0(moduleId, request)
          b2 <- moduleApprovalService.canApproveModule(moduleId, person.id)
        } yield b1 || b2
        toResult(hasPermission, request.request)
      }

      override protected def executionContext: ExecutionContext = ctx
    }
}
