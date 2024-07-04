import controllers.ErrorHandler
import play.api.Logging
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._

import javax.inject.Singleton
import scala.annotation.unused
import scala.concurrent._

@unused
@Singleton
class ErrorHandler extends HttpErrorHandler with Logging {

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
    logger.error(
      s"server error occurred on ${request.method} ${request.uri}",
      exception
    )
    Future.successful(
      ErrorHandler.internalServerError(
        request.toString(),
        exception.getMessage,
        exception
      )
    )
  }
}
