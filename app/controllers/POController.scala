package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import controllers.POController.validAttribute
import models.core.PO
import play.api.cache.Cached
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.POService

object POController {
  val validAttribute = "valid"
}

@Singleton
final class POController @Inject() (
    cc: ControllerComponents,
    service: POService,
    cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def all() =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action.async { request =>
        val validOnly = request
          .getQueryString(validAttribute)
          .flatMap(_.toBooleanOption)
          .getOrElse(true)
        val res =
          if (validOnly) service.allValid()
          else service.all()
        res.map(xs => Ok(Json.toJson(xs)))
      }
    }
}
