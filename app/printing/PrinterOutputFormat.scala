package printing

import scala.util.{Failure, Success, Try}

sealed trait PrinterOutputFormat

object PrinterOutputFormat {
  case object HTML extends PrinterOutputFormat
  case object HTMLStandalone extends PrinterOutputFormat
  case object HTMLFile extends PrinterOutputFormat
  case object HTMLStandaloneFile extends PrinterOutputFormat
  case object PDFFile extends PrinterOutputFormat
  case object PDFStandaloneFile extends PrinterOutputFormat

  def apply(string: String): Try[PrinterOutputFormat] =
    string.toLowerCase match {
      case "html" =>
        Success(HTML)
      case "html-standalone" =>
        Success(HTMLStandalone)
      case "html-file" =>
        Success(HTMLFile)
      case "html-standalone-file" | "html-file-standalone" =>
        Success(HTMLStandaloneFile)
      case "pdf" =>
        Success(PDFFile)
      case "pdf-standalone" =>
        Success(PDFStandaloneFile)
      case _ =>
        Failure(
          new Throwable(
            s"PrinterOutputFormat creation failed. expected html or pdf, but found $string"
          )
        )
    }
}
