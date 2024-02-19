package controllers

import play.api.libs.json.{Json, Writes}
import play.api.mvc.AbstractController
import service.core.YamlService

import scala.concurrent.ExecutionContext

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
