package printing

import scala.util.{Failure, Success, Try}

sealed trait PrinterOutputType {
  override def toString = this match {
    case PrinterOutputType.HTML               => "html"
    case PrinterOutputType.HTMLStandalone     => "html-standalone"
    case PrinterOutputType.HTMLFile           => "html-file"
    case PrinterOutputType.HTMLStandaloneFile => "html-standalone-file"
    case PrinterOutputType.PDFFile            => "pdf"
    case PrinterOutputType.PDFStandaloneFile  => "pdf-standalone"
  }
}

object PrinterOutputType {
  case object HTML extends PrinterOutputType
  case object HTMLStandalone extends PrinterOutputType
  case object HTMLFile extends PrinterOutputType
  case object HTMLStandaloneFile extends PrinterOutputType
  case object PDFFile extends PrinterOutputType
  case object PDFStandaloneFile extends PrinterOutputType

  def apply(string: String): Try[PrinterOutputType] =
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
            s"PrinterOutputType creation failed. expected ${all
                .mkString(" or ")}, but found $string"
          )
        )
    }

  def all: List[PrinterOutputType] = List(
    HTML,
    HTMLStandalone,
    HTMLFile,
    HTMLStandaloneFile,
    PDFFile,
    PDFStandaloneFile
  )
}
