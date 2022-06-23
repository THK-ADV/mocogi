package printing

import scala.util.{Failure, Success, Try}

sealed trait PrinterOutputFormat

object PrinterOutputFormat {
  case object HTML extends PrinterOutputFormat
  case object PDF extends PrinterOutputFormat

  def apply(string: String): Try[PrinterOutputFormat] =
    string.toLowerCase match {
      case "html" => Success(HTML)
      case "pdf"  => Success(PDF)
      case _ =>
        Failure(
          new Throwable(
            s"PrinterOutputFormat creation failed. expected html or pdf, but found $string"
          )
        )
    }
}
