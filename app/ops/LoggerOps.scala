package ops

import play.api.Logging

trait LoggerOps { self: Logging =>
  def measure[A](ctx: String, f: => A): A = {
    val start = System.currentTimeMillis()
    logger.info(s"${Thread.currentThread().getName} start with $ctx")
    val a = f
    logger.info(s"${Thread.currentThread().getName} end with $ctx")
    val end = System.currentTimeMillis()
    val time = end - start
    logger.info(s"${Thread.currentThread().getName} $ctx: $time")
    a
  }

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
