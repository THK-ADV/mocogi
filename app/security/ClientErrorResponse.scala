package security

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results.*
import play.api.Environment
import play.api.Mode

/** JSON error bodies: detailed in Dev, generic + message in Prod (mit Logging). */
@Singleton
final class ClientErrorResponse @Inject() (env: Environment) {

  private val log = LoggerFactory.getLogger(classOf[ClientErrorResponse])

  private def production: Boolean = env.mode == Mode.Prod

  private def nextId: String = UUID.randomUUID().toString

  def unauthorized(request: RequestHeader, cause: Throwable): Result = {
    val id = nextId
    log.warn(s"[$id] Unauthorized ${request.method} ${request.uri}", cause)
    if production then Unauthorized(Json.obj("type" -> "unauthorized", "message" -> id))
    else Unauthorized(Json.obj("request" -> request.toString, "message" -> cause.getMessage))
  }

  def forbidden(request: RequestHeader, message: String): Result = {
    val id = nextId
    log.warn(s"[$id] Forbidden ${request.method} ${request.uri}: $message")
    if production then Forbidden(Json.obj("type" -> "forbidden", "message" -> id))
    else Forbidden(Json.obj("request" -> request.toString, "message" -> message))
  }

  def badRequest(request: RequestHeader, message: String): Result = {
    val id = nextId
    log.warn(s"[$id] BadRequest ${request.method} ${request.uri}: $message")
    if production then BadRequest(Json.obj("type" -> "bad_request", "message" -> id))
    else BadRequest(Json.obj("request" -> request.toString, "message" -> message))
  }

  def badRequest(request: RequestHeader, throwable: Throwable): Result = {
    val t = if throwable.getCause != null then throwable.getCause else throwable
    badRequest(request, t.getMessage)
  }

  /** For [[play.api.http.HttpErrorHandler.onClientError]] (4xx from Play). */
  def clientStatusError(request: RequestHeader, statusCode: Int, message: String): Result = {
    val id = nextId
    log.warn(s"[$id] HTTP $statusCode ${request.method} ${request.uri}: $message")
    if production then Status(statusCode)(Json.obj("type" -> "client_error", "message" -> id))
    else Status(statusCode)(Json.obj("request" -> request.toString, "message" -> message))
  }

  def internalServerError(request: RequestHeader, throwable: Throwable): Result = {
    val id = nextId
    log.error(s"[$id] Server error ${request.method} ${request.uri}", throwable)
    val t   = if throwable.getCause != null then throwable.getCause else throwable
    val msg = Option(t.getMessage).getOrElse(t.getClass.getSimpleName)
    if production then InternalServerError(Json.obj("type" -> "server_error", "message" -> id))
    else
      InternalServerError(
        Json.obj(
          "type"    -> "server error",
          "request" -> request.toString,
          "message" -> msg
        )
      )
  }
}
