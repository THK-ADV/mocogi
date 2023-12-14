package ops

import play.api.Logging

import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

/*
https://chaitanyawaikar1993.medium.com/tracking-time-of-futures-in-scala-b64c71b965db
 */
class FutureTimeTracker[T](body: => Future[T])(implicit
    executionContext: ExecutionContext
) extends Logging {
  private val start = System.currentTimeMillis()

  @unused
  def track(tag: String): Future[T] = {
    body.andThen { case _ =>
      val end = System.currentTimeMillis()
      logger.info(s"Time Consumed by $tag is: ${end - start} millis")
    }
  }
}

@unused
object FutureTimeTracker {
  def apply[T](body: => Future[T])(implicit
      executionContext: ExecutionContext
  ): FutureTimeTracker[T] =
    new FutureTimeTracker(body)
}
