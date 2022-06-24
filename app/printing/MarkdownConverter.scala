package printing

import play.api.libs.Files.DefaultTemporaryFileCreator

import java.io.ByteArrayInputStream
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
      input: String,
      outputFormat: PrinterOutputFormat
  ): Either[Throwable, PrinterOutput] = {
    val inputStream = new ByteArrayInputStream(input.getBytes)
    val res = outputFormat match {
      case PrinterOutputFormat.HTML =>
        Try(htmlCmd #< inputStream !!)
          .map(PrinterOutput.HTML.apply)
          .toEither
      case PrinterOutputFormat.PDF =>
        val filename = s"${input.hashCode}.pdf"
        val file = fileCreator.create(filename)
        val cmd = s"$pdfCmd -o ${file.getAbsolutePath}"
        Try(cmd #< inputStream !!).map { _ =>
          PrinterOutput.PDF(file, filename)
        }.toEither
    }
    inputStream.close()
    res
  }

}
