package controllers

import auth.AuthorizationAction
import controllers.ModuleUpdatePermissionController.Replace
import controllers.actions.{ModuleUpdatePermissionCheck, PermissionCheck}
import models.CampusId
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ModuleUpdatePermissionService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

object ModuleUpdatePermissionController {
  case class Replace(permissions: List[CampusId]) extends AnyVal
  implicit def reads: Reads[Replace] = Json.reads
}

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
      hasPermissionToGrantPermission(moduleId) async { _ =>
        moduleUpdatePermissionService
          .allFromModule(moduleId)
          .map(xs => Ok(Json.toJson(xs)))
      }

  def replace(moduleId: UUID) =
    auth(parse.json[Replace]) andThen
      hasPermissionToGrantPermission(moduleId) async { r =>
        moduleUpdatePermissionService
          .replace(moduleId, r.body.permissions)
          .map(_ => NoContent)
      }
}
