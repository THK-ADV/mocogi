package controllers

import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.InternalServerError

object ErrorHandler {

  def internalServerError(
      request: String,
      throwable: Throwable
  ) = InternalServerError(
    Json.obj(
      "type"    -> "server error",
      "request" -> request,
      "message" -> throwable.getMessage
    )
  )

  def badRequest(
      request: String,
      throwable: Throwable
  ) = BadRequest(
    Json.obj(
      "request" -> request,
      "message" -> throwable.getMessage
    )
  )

  def badRequest(
      request: String,
      message: String
  ) = BadRequest(
    Json.obj(
      "request" -> request,
      "message" -> message
    )
  )
}
