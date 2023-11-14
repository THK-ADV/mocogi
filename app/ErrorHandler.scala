import play.api.http.HttpErrorHandler
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results._
import play.api.mvc._

import javax.inject.Singleton
import scala.annotation.unused
import scala.concurrent._

@unused
@Singleton
class ErrorHandler extends HttpErrorHandler {

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
  ): Future[Result] =
    Future.successful(
      InternalServerError(
        Json.obj(
          "type" -> "server error",
          "request" -> request.toString(),
          "message" -> exception.getMessage,
          "trace" -> exception.getStackTrace.mkString("\n")
        )
      )
    )
}
