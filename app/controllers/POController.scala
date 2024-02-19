package controllers

import controllers.POController.validAttribute
import models.core.PO
import play.api.libs.json.{Json, Writes}
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
) extends AbstractController(cc)
    with YamlController[PO] {
  override implicit val writes: Writes[PO] = PO.writes

  override def all() =
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
