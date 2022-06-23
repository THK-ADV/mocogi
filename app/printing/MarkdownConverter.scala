package printing

import play.api.libs.Files.DefaultTemporaryFileCreator
import printing.PrinterOutputFormat.{HTML, PDF}

import java.io.ByteArrayInputStream
import javax.inject.{Inject, Singleton}
import scala.language.postfixOps
import scala.sys.process._
import scala.util.Try

@Singleton
final class MarkdownConverter @Inject() (
    fileCreator: DefaultTemporaryFileCreator
) {
  def convert(
      input: String,
      outputFormat: PrinterOutputFormat
  ): Either[Throwable, PrinterOutput] = {
    val inputStream = new ByteArrayInputStream(input.getBytes)
    val res = outputFormat match {
      case HTML =>
        val cmd = "pandoc -f markdown -t html"
        Try(cmd #< inputStream !!)
          .map(PrinterOutput.HTML.apply)
          .toEither
      case PDF =>
        val filename = s"${input.hashCode}.pdf"
        val file = fileCreator.create(filename)
        val cmd =
          s"pandoc -f markdown -t pdf --pdf-engine=/Library/TeX/texbin/xelatex -V colorlinks=true -o ${file.getAbsolutePath}"
        Try(cmd #< inputStream !!).map { _ =>
          PrinterOutput.PDF(file, filename)
        }.toEither
    }
    inputStream.close()
    res
  }
}
