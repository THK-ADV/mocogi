package git.api

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.libs.ws.WSClient

// TODO extend usage of this class
@Singleton
final class GitAvailabilityChecker @Inject() (
    @Named("gitHost") val gitHost: String,
    val ws: WSClient,
    implicit val ctx: ExecutionContext
) {
  def checkAvailability(): Future[Unit] =
    ws.url(gitHost)
      .head()
      .flatMap { res =>
        if (res.status == 200) Future.unit
        else Future.failed(new Exception(s"Git Service status is: ${res.statusText}"))
      }
}
