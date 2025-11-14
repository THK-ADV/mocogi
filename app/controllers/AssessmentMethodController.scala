package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import controllers.actions.AdminCheck
import models.core.AssessmentMethod
import models.AssessmentMethodSource
import models.PermittedAssessmentMethodForModule
import play.api.cache.Cached
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.AssessmentMethodService

@Singleton
final class AssessmentMethodController @Inject() (
    cc: ControllerComponents,
    service: AssessmentMethodService,
    cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def all() =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action.async { r =>
        r.queryString match
          case query if query.get("module").exists(_.nonEmpty) =>
            service.allForModule(UUID.fromString(query("module").head)).map(xs => Ok(Json.toJson(xs)))
          case query if query.get("source").exists(_.contains(AssessmentMethodSource.RPO.id)) =>
            service.allRPO().map(xs => Ok(Json.toJson(xs)))
          case query if query.isEmpty =>
            service.all().map(xs => Ok(Json.toJson(xs)))
          case _ =>
            Future.successful(
              ErrorHandler.badRequest(
                r,
                s"unable to handle query parameter ${r.queryString}"
              )
            )
      }
    }
}
