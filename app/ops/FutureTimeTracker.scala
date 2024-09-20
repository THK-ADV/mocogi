package ops

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.Logging

/*
https://chaitanyawaikar1993.medium.com/tracking-time-of-futures-in-scala-b64c71b965db
 */
class FutureTimeTracker[T](body: => Future[T])(implicit executionContext: ExecutionContext) extends Logging {
  private val start = System.currentTimeMillis()

  def track(tag: String): Future[T] = {
    body.andThen {
      case _ =>
        val end = System.currentTimeMillis()
        logger.info(s"Time Consumed by $tag is: ${end - start} millis")
    }
  }
}
