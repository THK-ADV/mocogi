package controllers.actions

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.PersonAction.PersonRequest
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import service.ModuleApprovalService
import service.ModuleDraftService
import service.ModuleUpdatePermissionService

trait ModuleDraftCheck { self: PermissionCheck =>
  implicit def moduleDraftService: ModuleDraftService
  implicit def moduleUpdatePermissionService: ModuleUpdatePermissionService

  private def hasPermissionToEditDraft0[A](
      moduleId: UUID,
      request: PersonRequest[A]
  ): Future[Boolean] = {
    val campusId = request.request.campusId
    val person   = request.person
    for {
      b1 <- moduleUpdatePermissionService.hasPermission(campusId, moduleId)
      b2 <- if b1 then Future.successful(b1) else moduleDraftService.isAuthorOf(moduleId, person.id)
      b3 <-
        if b2 then Future.successful(b2)
        else moduleUpdatePermissionService.isModuleInPO(moduleId, request.request.token.roles)
    } yield b3
  }

  def hasPermissionToEditDraft(moduleId: UUID) =
    new ActionFilter[PersonRequest] {
      protected override def filter[A](
          request: PersonRequest[A]
      ): Future[Option[Result]] =
        toResult(hasPermissionToEditDraft0(moduleId, request), request.request)

      protected override def executionContext: ExecutionContext = ctx
    }

  def hasPermissionToViewDraft(
      moduleId: UUID,
      moduleApprovalService: ModuleApprovalService
  ) =
    new ActionFilter[PersonRequest] {
      protected override def filter[A](
          request: PersonRequest[A]
      ): Future[Option[Result]] = {
        val person = request.person
        val hasPermission = for {
          b1 <- hasPermissionToEditDraft0(moduleId, request)
          b2 <- if b1 then Future.successful(b1) else moduleApprovalService.canApproveModule(moduleId, person.id)
        } yield b2
        toResult(hasPermission, request.request)
      }

      protected override def executionContext: ExecutionContext = ctx
    }
}
