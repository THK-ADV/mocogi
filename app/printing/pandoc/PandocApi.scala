package printing.pandoc

import java.io.{ByteArrayInputStream, File}
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Singleton
import scala.language.{existentials, postfixOps}
import scala.sys.process._
import scala.util.{Failure, Success, Try}

@Singleton
final class PandocApi(
    htmlCmd: String,
    pdfCmd: String
) {

  private val htmlExtension = "html"

  private val pdfExtension = "pdf"

  def run(
      id: UUID,
      outputType: PrinterOutputType,
      input: String,
      outputFolderPath: String
  ): Either[Throwable, PrinterOutput] = {
    val inputStream = toStream(input)
    val res = outputType match {
      case PrinterOutputType.HTML =>
        createText(htmlCmd, htmlExtension, inputStream)
      case PrinterOutputType.HTMLStandalone =>
        createText(standalone(htmlCmd), htmlExtension, inputStream)
      case PrinterOutputType.HTMLFile =>
        createFile(id, htmlExtension, htmlCmd, inputStream, outputFolderPath)
      case PrinterOutputType.HTMLStandaloneFile =>
        createFile(
          id,
          htmlExtension,
          standalone(htmlCmd),
          inputStream,
          outputFolderPath
        )
      case PrinterOutputType.PDFFile =>
        createFile(id, pdfExtension, pdfCmd, inputStream, outputFolderPath)
      case PrinterOutputType.PDFStandaloneFile =>
        createFile(
          id,
          pdfExtension,
          standalone(pdfCmd),
          inputStream,
          outputFolderPath
        )
    }
    inputStream.close()
    res
  }

  private def toStream(input: String) =
    new ByteArrayInputStream(
      input.getBytes(StandardCharsets.UTF_8)
    )

  private def standalone(cmd: String): String = s"$cmd -s --embed-resource"

  private def createText(
      cmd: String,
      extension: String,
      inputStream: ByteArrayInputStream
  ): Either[Throwable, PrinterOutput] = {
    val process = cmd #< inputStream
    var output = ""
    val logger = ProcessLogger(_ => (), err => output += s"$err\n")
    Try(process !! logger) match {
      case Failure(e) =>
        Left(new Throwable(output, e))
      case Success(c) =>
        Right(PrinterOutput.Text(c, extension, output))
    }
  }

  private def createFile(
      id: UUID,
      extension: String,
      cmd: String,
      inputStream: ByteArrayInputStream,
      outputFolderPath: String
  ): Either[Throwable, PrinterOutput] = {
    val filename = s"$outputFolderPath/$id.$extension"
    val process = new File(filename) #< cmd #< inputStream
    var output = ""
    val logger = ProcessLogger(_ => (), err => output += s"$err\n")
    Try(process ! logger) match {
      case Failure(e) =>
        Left(new Throwable(output, e))
      case Success(_) =>
        Right(PrinterOutput.File(filename, output))
    }
  }
}
