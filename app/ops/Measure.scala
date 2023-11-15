package ops

import play.api.Logging

trait Measure { self: Logging =>
  def measure[A](ctx: String = "", f: => A): A = {
    val start = System.currentTimeMillis()
    val a = f
    val end = System.currentTimeMillis()
    val time = end - start
    if (ctx.isEmpty) logger.info(s"measured time: $time")
    else logger.info(s"$ctx: $time")
    a
  }
}
