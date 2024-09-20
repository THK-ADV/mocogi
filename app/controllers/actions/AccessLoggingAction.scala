package controllers.actions

import javax.inject.Inject
import javax.inject.Singleton

import scala.annotation.unused
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.mvc.ActionBuilderImpl
import play.api.mvc.BodyParsers
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.Logging

@Singleton
@unused
final class AccessLoggingAction @Inject() (@unused parser: BodyParsers.Default)(
    implicit ec: ExecutionContext
) extends ActionBuilderImpl(parser)
    with Logging {

  override def invokeBlock[A](
      request: Request[A],
      block: (Request[A]) => Future[Result]
  ) = {
    logger.info(
      s"method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}"
    )
    block(request)
  }
}
