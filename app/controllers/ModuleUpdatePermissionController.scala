package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import auth.CampusId
import controllers.actions.ModuleUpdatePermissionCheck
import controllers.actions.PermissionCheck
import models.ModuleUpdatePermissionType
import play.api.libs.json.JsPath
import play.api.libs.json.Json
import play.api.libs.json.JsonValidationError
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.IdentityService
import service.ModuleUpdatePermissionService

@Singleton
final class ModuleUpdatePermissionController @Inject() (
    cc: ControllerComponents,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val identityService: IdentityService,
    auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleUpdatePermissionCheck
    with PermissionCheck {

  def getOwn =
    auth.async { r =>
      moduleUpdatePermissionService
        .allFromUser(r.campusId)
        .map(xs => Ok(Json.toJson(xs)))
    }

  def allByModule(moduleId: UUID) =
    auth.andThen(hasInheritedPermission(moduleId)).async { r =>
      if r.isNewApi then moduleUpdatePermissionService.allGrantedFromModule(moduleId).map(Ok(_))
      else
        moduleUpdatePermissionService
          .allGrantedFromModule2(moduleId)
          .map(xs => Ok(Json.toJson(xs)))
    }

  def replace(moduleId: UUID) =
    auth(parse.json).andThen(hasInheritedPermission(moduleId)).async { r =>
      def go(ids: List[CampusId]) =
        moduleUpdatePermissionService
          .replace(moduleId, ids, ModuleUpdatePermissionType.Granted)
          .map(_ => NoContent)

      if r.isNewApi then
        r.body
          .validate[List[String]]
          .fold(
            respondWithError,
            ids => identityService.allByIds(ids).flatMap(go)
          )
      else r.body.validate[List[CampusId]].fold(respondWithError, go)
    }

  private def respondWithError(errs: Seq[(JsPath, Seq[JsonValidationError])]) =
    Future.successful(
      BadRequest(
        Json.obj(
          "message" -> "Invalid JSON format",
          "errors" -> errs
            .map {
              case (path, validationErrors) =>
                s"$path: ${validationErrors.map(_.message).mkString(", ")}"
            }
            .mkString("; ")
        )
      )
    )

}
