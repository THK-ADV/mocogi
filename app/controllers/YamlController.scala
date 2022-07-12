package controllers

import play.api.libs.json.{Json, Writes}
import play.api.mvc.AbstractController
import service.YamlService

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext

trait YamlController[A] { self: AbstractController =>

  implicit val writes: Writes[A]

  implicit val ctx: ExecutionContext

  val service: YamlService[A]

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
