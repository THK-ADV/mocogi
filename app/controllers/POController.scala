package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import controllers.POController.validAttribute
import models.core.PO
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
    val service: POService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[PO] {
  implicit override val writes: Writes[PO] = PO.writes

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
