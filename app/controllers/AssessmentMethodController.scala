package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.core.AssessmentMethodRepository
import database.repo.PermissionRepository
import models.core.AssessmentMethod
import models.AssessmentMethodSource
import permission.ModuleDraftCheck
import play.api.cache.Cached
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import security.ClientErrorResponse
import service.ModuleUpdatePermissionService

@Singleton
final class AssessmentMethodController @Inject() (
    cc: ControllerComponents,
    repo: AssessmentMethodRepository,
    cached: Cached,
    val clientErrors: ClientErrorResponse,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val permissionRepository: PermissionRepository,
    auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UsesClientErrors
    with ModuleDraftCheck
    with UserResolveAction {

  def all() =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action.async { r =>
        r.queryString match
          case query if query.get("source").exists(_.contains(AssessmentMethodSource.RPO.id)) =>
            repo.allRPO().map(xs => Ok(Json.toJson(xs)))
          case query if query.isEmpty =>
            repo.all().map(xs => Ok(Json.toJson(xs)))
          case _ =>
            Future.successful(
              clientErrors.badRequest(
                r,
                s"unable to handle query parameter ${r.queryString}"
              )
            )
      }
    }

  def counts() =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action.async { r => repo.moduleCountPerMethod().map(xs => Ok(Json.toJson(xs))) }
    }

  def allPermittedForModule(module: UUID) =
    Action.async(_ => repo.allPermittedForModuleOrDefault(module).map(xs => Ok(Json.toJson(xs))))

  def replaceForModule(module: UUID) =
    auth(parse.json[List[String]]).andThen(resolveUser).andThen(canEditModule(module)).async {
      (r: UserRequest[List[String]]) =>
        repo.replaceForModule(module, r.body).map(_ => NoContent).recover {
          case e: NoSuchElementException => BadRequest(Json.obj("message" -> e.getMessage))
        }
    }
}
