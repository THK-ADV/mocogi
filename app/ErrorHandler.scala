import javax.inject.Inject
import javax.inject.Singleton

import scala.annotation.unused
import scala.concurrent.*
import scala.util.Failure
import scala.util.Success

import auth.Authorization
import auth.Token
import controllers.ErrorHandler
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.mvc.Results.*
import play.api.Logging

@unused
@Singleton
class ErrorHandler @Inject() (auth: Authorization[Token]) extends HttpErrorHandler with Logging {

  def onClientError(
      request: RequestHeader,
      statusCode: Int,
      message: String
  ): Future[Result] =
    Future.successful(
      Status(statusCode)(
        Json.obj(
          "type"    -> "client error",
          "request" -> request.toString(),
          "message" -> message
        )
      )
    )

  def onServerError(
      request: RequestHeader,
      exception: Throwable
  ): Future[Result] = {
    auth.authorize(
      request.headers.get(Authorization.AuthorizationHeader)
    ) match {
      case Success(token) =>
        logger.error(
          s"server error occurred from ${token.username} on ${request.method} ${request.uri}",
          exception
        )
      case Failure(_) =>
        logger.error(
          s"server error occurred on ${request.method} ${request.uri}",
          exception
        )
    }
    Future.successful(
      ErrorHandler.internalServerError(
        request.toString(),
        exception
      )
    )
  }
}
