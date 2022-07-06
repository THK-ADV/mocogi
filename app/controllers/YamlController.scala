package controllers

import play.api.libs.json.{Json, Writes}
import play.api.mvc.AbstractController
import service.YamlService

import scala.concurrent.{ExecutionContext, Future}

trait YamlController[A] extends RequestBodyFileParser{ self: AbstractController =>

  implicit val writes: Writes[A]

  implicit val ctx: ExecutionContext

  val service: YamlService[A]

  def all() =
    Action.async { _ =>
      service.all().map(xs => Ok(Json.toJson(xs)))
    }

  def createFromYamlFile() =
    Action(parse.temporaryFile).async { r =>
      for {
        input <- Future.fromTry(parseFileContent(r))
        xs <- service.createFromYamlFile(input)
      } yield Ok(Json.toJson(xs))
    }
}
