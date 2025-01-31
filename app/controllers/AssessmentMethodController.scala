package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import models.core.AssessmentMethod
import models.AssessmentMethodSource
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

//  private def bootstrap = Action.async { _ =>
//    val elems = List(
//      PermittedAssessmentMethodForModule( // Algorithmik
//        UUID.fromString("21723454-3c3e-4ebe-ade0-82eacb69b185"),
//        List("written-exam", "written-exam-answer-choice-method", "oral-exam", "home-assignment", "open-book-exam")
//      ),
//      PermittedAssessmentMethodForModule( // PP
//        UUID.fromString("e37c5af9-6076-4f15-8c8b-d206b7091bc0"),
//        List("written-exam", "home-assignment")
//      )
//    )
//    moduleAssessmentMethodRepo.insert(elems).map(_ => NoContent)
//  }
}
