package controllers

import auth.{AuthorizationAction, CampusId}
import controllers.actions.{ModuleUpdatePermissionCheck, PermissionCheck}
import models.ModuleUpdatePermissionType
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ModuleUpdatePermissionService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleUpdatePermissionController @Inject() (
    cc: ControllerComponents,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleUpdatePermissionCheck
    with PermissionCheck {

  def getOwn =
    auth async { r =>
      moduleUpdatePermissionService
        .allFromUser(r.campusId)
        .map(xs => Ok(Json.toJson(xs)))
    }

  def allByModule(moduleId: UUID) =
    auth andThen
      hasInheritedPermission(moduleId) async { _ =>
        moduleUpdatePermissionService
          .allGrantedFromModule(moduleId)
          .map(xs => Ok(Json.toJson(xs)))
      }

  def replace(moduleId: UUID) =
    auth(parse.json[List[CampusId]]) andThen
      hasInheritedPermission(moduleId) async { r =>
        moduleUpdatePermissionService
          .replace(moduleId, r.body, ModuleUpdatePermissionType.Granted)
          .map(_ => NoContent)
      }
}
