package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.ModuleDraftCheck
import controllers.actions.PermissionCheck
import controllers.actions.PersonAction
import database.repo.core.IdentityRepository
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.ModuleDraftService
import service.ModuleReviewService
import service.ModuleUpdatePermissionService

@Singleton
final class ModuleDraftReviewController @Inject() (
    cc: ControllerComponents,
    val auth: AuthorizationAction,
    val service: ModuleReviewService,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val moduleDraftService: ModuleDraftService,
    val identityRepository: IdentityRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleDraftCheck
    with PermissionCheck
    with PersonAction {

  /**
   * Creates a full review.
   * @param moduleId
   *   ID of the module draft
   * @return
   *   201 Created
   */
  def create(moduleId: UUID) =
    auth.andThen(personAction).andThen(hasPermissionToEditDraft(moduleId)).async { r =>
      service.create(moduleId, r.person).map(_ => Created)
    }

  /**
   * Deletes a full review.
   * @param moduleId
   *   ID of the module draft
   * @return
   *   204 NoContent
   */
  def delete(moduleId: UUID) =
    auth.andThen(personAction).andThen(hasPermissionToEditDraft(moduleId)).async { _ =>
      service.delete(moduleId).map(_ => NoContent)
    }
}
