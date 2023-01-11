package controllers

import controllers.POController.validAttribute
import controllers.json.POFormat
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.POService

import java.nio.charset.StandardCharsets
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
) extends AbstractController(cc)
    with POFormat {

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

  def create() =
    Action(parse.byteString).async { r =>
      val input = r.body.decodeString(StandardCharsets.UTF_8)
      service.create(input).map(xs => Ok(Json.toJson(xs)))
    }
}
