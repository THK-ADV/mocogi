package controllers

import play.api.libs.json.{Json, Writes}
import play.api.mvc.AbstractController
import service.core.YamlService

import scala.concurrent.ExecutionContext

trait YamlController[Input, Output] { self: AbstractController =>

  implicit val writes: Writes[Output]

  implicit val ctx: ExecutionContext

  val service: YamlService[Input, Output]

  def all() =
    Action.async { _ =>
      service.all().map { xs =>
        Ok(Json.toJson(xs))
      }
    }
}
