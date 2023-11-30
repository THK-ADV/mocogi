package controllers

import play.api.libs.json.Json
import play.api.mvc.Results.InternalServerError

object ErrorHandler {
  def internalServerError(
      request: String,
      message: String,
      trace: Array[StackTraceElement]
  ) =
    InternalServerError(
      Json.obj(
        "type" -> "server error",
        "request" -> request,
        "message" -> message,
        "trace" -> trace.mkString("\n")
      )
    )
}
