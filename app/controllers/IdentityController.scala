package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import play.api.libs.json.*
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import database.repo.core.IdentityRepository
import models.core.Identity
import models.EmploymentType

@Singleton
final class IdentityController @Inject() (
    cc: ControllerComponents,
    repo: IdentityRepository,
    val cache: ResourceCache,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {
  def all() =
    cache("identities", 1.hour) {
      Action.async { r =>
        val withImages     = r.getQueryString("images").flatMap(_.toBooleanOption).getOrElse(false)
        val employmentType = r.getQueryString("employmentType").map(EmploymentType.apply)
        if withImages then
          repo.allWithImages(employmentType).map { entries =>
            val json = entries.map {
              case (entry, image) =>
                Json.toJson(Identity.fromDbEntry(entry)).as[JsObject] +
                  ("imageUrl" -> image.fold[JsValue](JsNull)(i => JsString(i.imageUrl)))
            }
            Ok(JsArray(json))
          }
        else repo.list(employmentType).map(xs => Ok(Json.toJson(xs)))
      }
    }
}
