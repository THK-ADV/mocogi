package controllers

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import play.api.cache.Cached
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import service.core.YamlService

private[controllers] trait YamlController[A] { self: AbstractController =>

  implicit val writes: Writes[A]

  implicit val ctx: ExecutionContext

  def service: YamlService[A]

  def cached: Cached

  def all() =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action.async { _ =>
        service.all().map(xs => Ok(Json.toJson(xs)))
      }
    }
}
