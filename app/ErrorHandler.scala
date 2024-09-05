import auth.{Authorization, UserToken}
import controllers.ErrorHandler
import play.api.Logging
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent._
import scala.util.{Failure, Success}

@unused
@Singleton
class ErrorHandler @Inject() (auth: Authorization[UserToken])
    extends HttpErrorHandler
    with Logging {

  def onClientError(
      request: RequestHeader,
      statusCode: Int,
      message: String
  ): Future[Result] =
    Future.successful(
      Status(statusCode)(
        Json.obj(
          "type" -> "client error",
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
      case Success(userToken) =>
        logger.error(
          s"server error occurred from ${userToken.username} on ${request.method} ${request.uri}",
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
