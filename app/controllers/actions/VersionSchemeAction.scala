package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.PersonAction.PersonRequest
import parser.ParsingError
import parsing.metadata.VersionSchemeParser
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc.ActionRefiner
import play.api.mvc.Results.BadRequest

final class VersionSchemeAction(key: String)(
    implicit val executionContext: ExecutionContext,
    writes: Writes[ParsingError]
) extends ActionRefiner[PersonRequest, VersionSchemeRequest] {

  def refine[A](input: PersonRequest[A]) = Future.successful {
    input.headers.get(key) match {
      case Some(str) =>
        VersionSchemeParser.parser.parse(str)._1 match {
          case Left(err)    => Left(BadRequest(Json.toJson(err)))
          case Right(value) => Right(VersionSchemeRequest(value, input))
        }
      case None =>
        Left(BadRequest(Json.obj("message" -> s"expected header $key")))
    }
  }
}
