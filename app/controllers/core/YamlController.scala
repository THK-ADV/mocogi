package controllers.core

import play.api.libs.json.{Json, Writes}
import play.api.mvc.AbstractController
import service.core.YamlService

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext

trait YamlController[Input, Output] { self: AbstractController =>

  implicit val writesOut: Writes[Output]

  implicit val writesIn: Writes[Input]

  implicit val ctx: ExecutionContext

  val service: YamlService[Input, Output]

  implicit def seqWrites[W](implicit w: Writes[W]): Writes[Seq[W]] =
    Writes.seq[W](w)

  def all() =
    Action.async { _ =>
      service.all().map { xs =>
        Ok(Json.toJson(xs))
      }
    }

  def create() =
    Action(parse.byteString).async { r =>
      val input = r.body.decodeString(StandardCharsets.UTF_8)
      service.create(input).map(xs => Ok(Json.toJson(xs)))
    }
}