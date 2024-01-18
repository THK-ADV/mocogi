package git.api

import play.api.libs.ws.WSClient

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
        else
          Future.failed(
            new Throwable(s"Git Service status is: ${res.statusText}")
          )
      }
}
