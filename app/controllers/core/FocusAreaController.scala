package controllers.core

import controllers.formats.FocusAreaFormat
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.FocusAreaService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class FocusAreaController @Inject() (
    cc: ControllerComponents,
    val service: FocusAreaService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with FocusAreaFormat {
  def all() =
    Action.async { _ =>
      service.all().map(xs => Ok(Json.toJson(xs)))
    }
}
