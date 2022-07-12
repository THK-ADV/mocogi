package printing

import play.api.libs.Files.DefaultTemporaryFileCreator

import java.io.ByteArrayInputStream
import java.util.UUID
import javax.inject.Singleton
import scala.language.postfixOps
import scala.sys.process._
import scala.util.Try

@Singleton
final class MarkdownConverter(
    fileCreator: DefaultTemporaryFileCreator,
    htmlCmd: String,
    pdfCmd: String
) {

  private val htmlExtension = "html"

  private val pdfExtension = "pdf"

  def convert(
      id: UUID,
      input: String,
      outputType: PrinterOutputType
  ): Either[Throwable, PrinterOutput] = {
    val inputStream = new ByteArrayInputStream(input.getBytes)
    val res = outputType match {
      case PrinterOutputType.HTML =>
        createText(htmlCmd, htmlExtension, inputStream)
      case PrinterOutputType.HTMLStandalone =>
        createText(standalone(htmlCmd), htmlExtension, inputStream)
      case PrinterOutputType.HTMLFile =>
        createFile(id, htmlExtension, htmlCmd, inputStream)
      case PrinterOutputType.HTMLStandaloneFile =>
        createFile(id, htmlExtension, standalone(htmlCmd), inputStream)
      case PrinterOutputType.PDFFile =>
        createFile(id, pdfExtension, pdfCmd, inputStream)
      case PrinterOutputType.PDFStandaloneFile =>
        createFile(id, pdfExtension, standalone(pdfCmd), inputStream)
    }
    inputStream.close()
    res
  }

  private def standalone(cmd: String): String = s"$cmd -s"

  private def createText(
      cmd: String,
      extension: String,
      inputStream: ByteArrayInputStream
  ): Either[Throwable, PrinterOutput] =
    Try(cmd #< inputStream !!)
      .map(c => PrinterOutput.Text(c, extension))
      .toEither

  private def createFile(
      id: UUID,
      extension: String,
      cmd: String,
      inputStream: ByteArrayInputStream
  ): Either[Throwable, PrinterOutput] = {
    val filename = s"output/$id.$extension" // TODO
    Try(s"$cmd -o $filename" #< inputStream !!)
      .map(_ => PrinterOutput.File(filename))
      .toEither
  }
}
