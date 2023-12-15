package ops

import play.api.Logging

import scala.annotation.unused

trait LoggerOps { self: Logging =>

  @unused
  def measure[A](tag: String, f: => A): A = {
    val start = System.currentTimeMillis()
    val a = f
    val end = System.currentTimeMillis()
    val time = end - start
    logger.info(s"Time Consumed by $tag is: $time")
    a
  }

  @unused
  def log[A](a: A): A = {
    logger.info(a.toString)
    a
  }

  def logSuccess(msg: String): Unit =
    logger.info(s"success\n  - message: $msg")

  def logFailure(error: Throwable): Unit =
    logger.error(s"""failed
         |  - message: ${error.getMessage}
         |  - trace: ${error.getStackTrace.mkString(
                     "\n           "
                   )}""".stripMargin)
}
