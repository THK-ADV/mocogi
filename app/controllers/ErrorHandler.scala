package controllers

import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.InternalServerError

object ErrorHandler {

  private def getCause(t: Throwable) =
    if t.getCause != null then t.getCause else t

  def internalServerError(
      request: String,
      throwable: Throwable
  ) =
    InternalServerError(
      Json.obj(
        "type"    -> "server error",
        "request" -> request,
        "message" -> getCause(throwable).getMessage
      )
    )

  def badRequest(
      request: String,
      throwable: Throwable
  ) = BadRequest(
    Json.obj(
      "request" -> request,
      "message" -> getCause(throwable).getMessage
    )
  )

  def badRequest(
      request: String | Request[AnyContent],
      message: String
  ) = {
    val requestStr = request match
      case r: String              => r
      case r: Request[AnyContent] => r.toString

    BadRequest(
      Json.obj(
        "request" -> requestStr,
        "message" -> message
      )
    )
  }
}
