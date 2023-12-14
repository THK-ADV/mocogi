package controllers.actions

import play.api.Logging
import play.api.mvc.{ActionBuilderImpl, BodyParsers, Request, Result}

import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

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
