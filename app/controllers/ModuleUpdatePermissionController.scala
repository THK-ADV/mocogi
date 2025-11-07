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
import database.repo.PermissionRepository
import models.ModuleUpdatePermissionType
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.ModuleDraftService
import service.ModuleUpdatePermissionService

@Singleton
final class ModuleUpdatePermissionController @Inject() (
    cc: ControllerComponents,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val moduleDraftService: ModuleDraftService,
    val permissionRepository: PermissionRepository,
    identityRepository: IdentityRepository,
    auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleDraftCheck
    with PersonAction
    with PermissionCheck {

  def getOwn =
    auth.async { r =>
      moduleUpdatePermissionService
        .allFromUser(r.campusId)
        .map(xs => Ok(Json.toJson(xs)))
    }

  def allByModule(moduleId: UUID) =
    auth.andThen(personAction).andThen(hasPermissionToEditDraft(moduleId)).async { r =>
      moduleUpdatePermissionService.allGrantedFromModule(moduleId).map(Ok(_))
    }

  def replace(moduleId: UUID) =
    auth(parse.json[List[String]]).andThen(personAction).andThen(hasPermissionToEditDraft(moduleId)).async { r =>
      for {
        ids <- identityRepository.allByIds(r.body)
        _   <- moduleUpdatePermissionService.replace(moduleId, ids, ModuleUpdatePermissionType.Granted)
      } yield NoContent
    }
}
