package ops

import java.io.PrintWriter
import java.io.StringWriter

import scala.annotation.unused

import play.api.Logging

trait LoggerOps { self: Logging =>

  @unused
  protected def measure[A](tag: String, f: => A): A = {
    val start = System.currentTimeMillis()
    val a     = f
    val end   = System.currentTimeMillis()
    val time  = end - start
    logger.info(s"Time Consumed by $tag is: $time")
    a
  }

  @unused
  protected def log[A](a: A): A = {
    logger.info(a.toString)
    a
  }

  protected def logStackTrace(t: Throwable): Unit = {
    val writer = new StringWriter
    t.printStackTrace(new PrintWriter(writer))
    logger.error(writer.toString)
  }

  protected def logSuccess(msg: String): Unit =
    logger.info(s"success\n  - message: $msg")

  protected def logFailure(error: Throwable): Unit =
    logger.error(s"""failed
                    |  - message: ${error.getMessage}
                    |  - trace: ${error.getStackTrace.mkString(
                     "\n           "
                   )}""".stripMargin)
}
