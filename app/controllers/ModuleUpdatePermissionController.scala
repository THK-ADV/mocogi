package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.UserResolveAction
import database.repo.core.IdentityRepository
import database.repo.PermissionRepository
import models.ModuleUpdatePermissionType
import permission.ModuleDraftCheck
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.ModuleUpdatePermissionService

@Singleton
final class ModuleUpdatePermissionController @Inject() (
    cc: ControllerComponents,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val permissionRepository: PermissionRepository,
    identityRepository: IdentityRepository,
    auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleDraftCheck
    with UserResolveAction {

  def allByModule(moduleId: UUID) =
    auth.andThen(resolveUser).andThen(canEditModule(moduleId)).async { r =>
      moduleUpdatePermissionService.allGrantedFromModule(moduleId).map(Ok(_))
    }

  def replace(moduleId: UUID) =
    auth(parse.json[List[String]]).andThen(resolveUser).andThen(canEditModule(moduleId)).async { r =>
      for {
        ids <- identityRepository.allByIds(r.body)
        _   <- moduleUpdatePermissionService.replace(moduleId, ids, ModuleUpdatePermissionType.Granted)
      } yield NoContent
    }
}
