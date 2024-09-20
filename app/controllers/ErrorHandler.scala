package controllers

import java.io.PrintWriter
import java.io.StringWriter

import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.InternalServerError

object ErrorHandler {

  private def trace(
      error: Throwable
  ) = {
    val writer = new StringWriter
    error.printStackTrace(new PrintWriter(writer))
    writer.toString
  }

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
