package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import auth.Role
import controllers.actions.PermissionCheck
import controllers.actions.RoleCheck
import models.core.Identity
import play.api.cache.Cached
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.IdentityService
import service.PeopleImagesService

@Singleton
final class IdentityController @Inject() (
    cc: ControllerComponents,
    val service: IdentityService,
    val cached: Cached,
    val peopleImagesService: PeopleImagesService,
    auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[Identity]
    with PermissionCheck
    with RoleCheck {
  implicit override val writes: Writes[Identity] = Identity.writes

  override def all() =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action.async { r =>
        val withImages = r.getQueryString("images").flatMap(_.toBooleanOption).getOrElse(false)
        if withImages then service.allWithImages().map(Ok(_)) else service.all().map(xs => Ok(Json.toJson(xs)))
      }
    }

  def updateAllImages() =
    auth.andThen(hasRole(Role.Admin)).async { _ =>
      peopleImagesService.updateAll().map(_ => NoContent)
    }
}
