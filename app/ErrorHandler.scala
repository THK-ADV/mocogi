import play.api.http.HttpErrorHandler
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results._
import play.api.mvc._

import javax.inject.Singleton
import scala.concurrent._

@Singleton
class ErrorHandler extends HttpErrorHandler {

  private def makeJson(
      `type`: String,
      request: RequestHeader,
      message: String
  ): JsValue =
    Json.obj(
      "type" -> `type`,
      "request" -> request.toString(),
      "message" -> message
    )

  def onClientError(
      request: RequestHeader,
      statusCode: Int,
      message: String
  ): Future[Result] =
    Future.successful(
      Status(statusCode)(
        makeJson("client error", request, message)
      )
    )

  def onServerError(
      request: RequestHeader,
      exception: Throwable
  ): Future[Result] =
    Future.successful(
      InternalServerError(
        makeJson("server error", request, exception.getMessage)
      )
    )
}
