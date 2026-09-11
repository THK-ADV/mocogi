package controllers

import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.UserResolveAction
import database.repo.PermissionRepository
import permission.AdminCheck
import permission.Permission
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import security.ClientErrorResponse

@Singleton
final class PermissionController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AdminCheck
    with UserResolveAction {

  private def admin = auth.andThen(resolveUser).andThen(isAdmin)

  def all() =
    admin.async(_ => permissionRepository.all().map(xs => Ok(Json.toJson(xs))))

  def create() =
    admin.async(parse.json[Permission]) { r =>
      permissionRepository.create(r.body).map(_ => Created).recover(conflict)
    }

  /** Only the context of a permission can be changed. */
  def update(id: Long) =
    admin.async(parse.json(Reads.optionWithNull[List[String]])) { r =>
      permissionRepository.updateContext(id, r.body).map(n => if n == 0 then NotFound else NoContent)
    }

  def delete(id: Long) =
    admin.async(_ => permissionRepository.delete(id).map(n => if n == 0 then NotFound else NoContent))

  private def conflict: PartialFunction[Throwable, Result] = {
    case e: SQLException if e.getSQLState == "23505" =>
      Conflict(Json.obj("message" -> "Die Person hat bereits eine Berechtigung dieses Typs"))
    case e: SQLException if e.getSQLState == "23503" =>
      Conflict(Json.obj("message" -> "Die Person existiert nicht"))
  }
}
