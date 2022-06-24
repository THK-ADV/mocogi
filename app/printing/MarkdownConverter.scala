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
  def convert(
      id: UUID,
      input: String,
      outputFormat: PrinterOutputFormat
  ): Either[Throwable, PrinterOutput] = {
    val inputStream = new ByteArrayInputStream(input.getBytes)
    val res = outputFormat match {
      case PrinterOutputFormat.HTML =>
        createText(htmlCmd, inputStream)
      case PrinterOutputFormat.HTMLStandalone =>
        createText(standalone(htmlCmd), inputStream)
      case PrinterOutputFormat.HTMLFile =>
        createFile(id, "html", htmlCmd, inputStream)
      case PrinterOutputFormat.HTMLStandaloneFile =>
        createFile(id, "html", standalone(htmlCmd), inputStream)
      case PrinterOutputFormat.PDFFile =>
        createFile(id, "pdf", pdfCmd, inputStream)
      case PrinterOutputFormat.PDFStandaloneFile =>
        createFile(id, "pdf", standalone(pdfCmd), inputStream)
    }
    inputStream.close()
    res
  }

  private def standalone(cmd: String): String = s"$cmd -s"

  private def createText(
      cmd: String,
      inputStream: ByteArrayInputStream
  ): Either[Throwable, PrinterOutput] =
    Try(cmd #< inputStream !!)
      .map(PrinterOutput.Text.apply)
      .toEither

  private def createFile(
      id: UUID,
      extension: String,
      cmd: String,
      inputStream: ByteArrayInputStream
  ): Either[Throwable, PrinterOutput] = {
    val filename = s"$id.$extension"
    val file = fileCreator.create(filename)
    Try(s"$cmd -o ${file.getAbsolutePath}" #< inputStream !!)
      .map(_ => PrinterOutput.File(file, filename))
      .toEither
  }
}
