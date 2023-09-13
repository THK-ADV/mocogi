package controllers.actions

import auth.UserTokenRequest
import parser.ParsingError
import parsing.metadata.VersionSchemeParser
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ActionRefiner, Headers}

import scala.concurrent.{ExecutionContext, Future}

final class VersionSchemeAction(key: String)(implicit
    val executionContext: ExecutionContext,
    writes: Writes[ParsingError]
) extends ActionRefiner[UserTokenRequest, VersionSchemeRequest] {

  def refine[A](input: UserTokenRequest[A]) = Future.successful {
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
