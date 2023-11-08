package controllers

import auth.AuthorizationAction
import controllers.actions.{ModuleDraftCheck, PermissionCheck}
import models.User
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{
  ModuleDraftService,
  ModuleReviewService,
  ModuleUpdatePermissionService
}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleDraftReviewController @Inject() (
    cc: ControllerComponents,
    val auth: AuthorizationAction,
    val service: ModuleReviewService,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val moduleDraftService: ModuleDraftService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleDraftCheck
    with PermissionCheck {

  /** Creates a full review.
    * @param moduleId
    *   ID of the module draft
    * @return
    *   201 Created
    */
  def create(moduleId: UUID) =
    auth andThen hasPermissionToEditDraft(moduleId) async { r =>
      service.create(moduleId, User(r.token.username)).map(_ => Created)
    }

  /** Deletes a full review.
    * @param moduleId
    *   ID of the module draft
    * @return
    *   204 NoContent
    */
  def delete(moduleId: UUID) =
    auth andThen hasPermissionToEditDraft(moduleId) async { _ =>
      service.delete(moduleId).map(_ => NoContent)
    }
}
