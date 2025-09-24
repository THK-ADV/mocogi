import java.time.format.DateTimeFormatter

import models.core.*

// TODO: remove if needed
package object printing {
  def localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

  def fmtDouble(d: Double): String =
    if (d % 1 == 0) d.toInt.toString
    else d.toString.replace('.', ',')

  def fmtIdentity(p: Identity): String =
    p match {
      case s: Identity.Person =>
        val base = s"${s.fullName} (${fmtCommaSeparated(s.faculties)(_.toUpperCase)})"
        if s.title.nonEmpty then s"${s.title} $base" else base
      case g: Identity.Group =>
        g.label
      case u: Identity.Unknown =>
        u.label
    }

  def fmtCommaSeparated[A](xs: Seq[A], sep: String = ", ")(
      f: A => String
  ): String = {
    val builder = new StringBuilder()
    xs.zipWithIndex.foreach {
      case (a, i) =>
        builder.append(f(a))
        if (i < xs.size - 1)
          builder.append(sep)
    }
    builder.toString()
  }
}
