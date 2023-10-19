package controllers

import auth.AuthorizationAction
import controllers.actions.{ModuleDraftCheck, PermissionCheck}
import models.User
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{
  ModuleDraftReviewService,
  ModuleDraftService,
  ModuleUpdatePermissionService
}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleDraftReviewController @Inject() (
    cc: ControllerComponents,
    val auth: AuthorizationAction,
    val reviewService: ModuleDraftReviewService,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val moduleDraftService: ModuleDraftService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleDraftCheck
    with PermissionCheck {
  def create(moduleId: UUID) =
    auth andThen hasPermissionToEditDraft(moduleId) async { r =>
      reviewService
        .create(moduleId, User(r.token.username))
        .map(_ => Created)
    }

  def delete(moduleId: UUID) =
    auth andThen hasPermissionToEditDraft(moduleId) async { r =>
      reviewService
        .close(moduleId)
        .map(_ => NoContent)
    }
}
