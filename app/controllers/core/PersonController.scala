package controllers.core

import controllers.formats.PersonFormat
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.PersonService

import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class PersonController @Inject() (
    cc: ControllerComponents,
    val service: PersonService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with PersonFormat {
  def all() =
    Action.async { _ =>
      service.all().map(xs => Ok(Json.toJson(xs)))
    }

  def create() =
    Action(parse.byteString).async { r =>
      val input = r.body.decodeString(StandardCharsets.UTF_8)
      service.create(input).map(xs => Ok(Json.toJson(xs)))
    }
}
