import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.*

import play.api.http.HttpErrorHandler
import play.api.mvc.*
import security.ClientErrorResponse

@Singleton
class ErrorHandler @Inject() (clientErrors: ClientErrorResponse) extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    Future.successful(clientErrors.clientStatusError(request, statusCode, message))

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
    Future.successful(clientErrors.internalServerError(request, exception))
}
