package controllers

import scala.concurrent.ExecutionContext

import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import service.core.YamlService

trait YamlController[A] { self: AbstractController =>

  implicit val writes: Writes[A]

  implicit val ctx: ExecutionContext

  val service: YamlService[A]

  def all() =
    Action.async { _ =>
      service.all().map { xs =>
        Ok(Json.toJson(xs))
      }
    }
}
