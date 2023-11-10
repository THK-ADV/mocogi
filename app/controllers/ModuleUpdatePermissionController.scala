package controllers

import auth.AuthorizationAction
import controllers.actions.{ModuleUpdatePermissionCheck, PermissionCheck}
import models.CampusId
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ModuleUpdatePermissionService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

object ModuleUpdatePermissionController {
  case class ModuleUpdatePermissionProtocol(module: UUID, campusId: CampusId)

  implicit val reads: Reads[ModuleUpdatePermissionProtocol] =
    Json.reads[ModuleUpdatePermissionProtocol]
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

  import ModuleUpdatePermissionController._

  def getOwn =
    auth async { r =>
      moduleUpdatePermissionService
        .all(r.campusId)
        .map(xs => Ok(Json.toJson(xs)))
    }

  def create() =
    auth(parse.json[ModuleUpdatePermissionProtocol]) andThen
      hasPermissionToGrantPermission async { r =>
        moduleUpdatePermissionService
          .createGranted(r.body.module, r.body.campusId)
          .map(_ => NoContent)
      }

  def delete() =
    auth(parse.json[ModuleUpdatePermissionProtocol]) andThen
      hasPermissionToGrantPermission async { r =>
        moduleUpdatePermissionService
          .removeGranted(r.body.module, r.body.campusId)
          .map(_ => NoContent)
      }
}
