package controllers

import controllers.POController.validAttribute
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.POService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

object POController {
  val validAttribute = "valid"
}

@Singleton
final class POController @Inject() (
    cc: ControllerComponents,
    val service: POService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def all() =
    Action.async { request =>
      val validOnly = request
        .getQueryString(validAttribute)
        .flatMap(_.toBooleanOption)
        .getOrElse(false)
      val res =
        if (validOnly) service.allValid()
        else service.all()
      res.map(xs => Ok(Json.toJson(xs)))
    }
}
