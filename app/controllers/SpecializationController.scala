package controllers

import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.SpecializationService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class SpecializationController @Inject() (
    cc: ControllerComponents,
    val service: SpecializationService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {
  def all() =
    Action.async { _ =>
      service.all().map(xs => Ok(Json.toJson(xs)))
    }
}
