package git.api

import git.GitConfig
import play.api.libs.ws.WSClient

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// TODO extend usage of this class
@Singleton
final class GitAvailabilityChecker @Inject() (implicit
    val ctx: ExecutionContext,
    val config: GitConfig,
    val ws: WSClient
) extends GitService {
  def checkAvailability() =
    ws.url(config.baseUrl)
      .withHttpHeaders(tokenHeader())
      .head()
      .flatMap(res =>
        if (res.status >= 200 && res.status < 300) Future.unit
        else
          Future.failed(
            new Throwable(s"Git Service status is: ${res.statusText}")
          )
      )
}
