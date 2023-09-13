package controllers

import auth.AuthorizationAction
import models.{ModuleUpdatePermission, ModuleUpdatePermissionType, User}
import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ModuleUpdatePermissionService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleUpdatePermissionController @Inject() (
    cc: ControllerComponents,
    service: ModuleUpdatePermissionService,
    auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  case class ModuleUpdatePermissionProtocol(module: UUID, user: String)

  implicit val userWrites: Writes[User] =
    Writes.of[String].contramap(_.username)

  implicit val typeWrites: Writes[ModuleUpdatePermissionType] =
    Writes.of[String].contramap(_.id)

  implicit val writes: Writes[ModuleUpdatePermission] =
    Json.writes[ModuleUpdatePermission]

  implicit val reads: Reads[ModuleUpdatePermissionProtocol] =
    Json.reads[ModuleUpdatePermissionProtocol]

  def all() =
    auth async { _ =>
      service.getAll().map(xs => Ok(Json.toJson(xs)))
    }

  // TODO add permission handling
  def create() =
    auth(parse.json[ModuleUpdatePermissionProtocol]).async { r =>
      service.createGranted(r.body.module, User(r.body.user)).map {
        case (id, user, updateType) =>
          Ok(
            Json.obj(
              "module" -> id,
              "user" -> user.username,
              "moduleUpdatePermissionType" -> updateType.id
            )
          )
      }
    }

  // TODO add permission handling
  def delete() =
    auth(parse.json[ModuleUpdatePermissionProtocol]).async { r =>
      service
        .removeGranted(r.body.module, User(r.body.user))
        .map(_ => NoContent)
    }
}
