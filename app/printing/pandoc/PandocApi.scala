package printing.pandoc

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Singleton

import scala.sys.process.*
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import printing.PrintingLanguage

@Singleton
final class PandocApi(
    htmlCmd: String,
    pdfCmd: String,
    texCmd: String
) {

  private val htmlExtension = "html"

  private val pdfExtension = "pdf"

  def toLatex(
      input: String
  ): Either[(Throwable, String), String] = {
    val inputStream = toStream(input)
    val process     = texCmd #< inputStream
    val sdtErr      = new StringBuilder()
    val logger      = ProcessLogger(_ => {}, sdtErr.append(_))
    try {
      Right(process !! logger)
    } catch {
      case NonFatal(e) =>
        Left(e, sdtErr.toString())
    }
  }

  def run(
      id: UUID,
      outputType: PrinterOutputType,
      input: String,
      lang: PrintingLanguage
  ): Either[Throwable, PrinterOutput] = {
    val inputStream = toStream(input)
    val res = outputType match {
      case PrinterOutputType.HTML =>
        createText(htmlCmd, htmlExtension, inputStream)
      case PrinterOutputType.HTMLStandalone =>
        createText(standalone(htmlCmd), htmlExtension, inputStream)
      case PrinterOutputType.HTMLFile(de, en) =>
        createFile(id, htmlExtension, htmlCmd, inputStream, lang.fold(de, en))
      case PrinterOutputType.HTMLStandaloneFile(de, en) =>
        createFile(
          id,
          htmlExtension,
          standalone(htmlCmd),
          inputStream,
          lang.fold(de, en)
        )
      case PrinterOutputType.PDFFile(de, en) =>
        createFile(id, pdfExtension, pdfCmd, inputStream, lang.fold(de, en))
      case PrinterOutputType.PDFStandaloneFile(de, en) =>
        createFile(
          id,
          pdfExtension,
          standalone(pdfCmd),
          inputStream,
          lang.fold(de, en)
        )
    }
    inputStream.close()
    res
  }

  private def toStream(input: String) =
    new ByteArrayInputStream(
      input.getBytes(StandardCharsets.UTF_8)
    )

  private def standalone(cmd: String): String = s"$cmd -s"

  private def createText(
      cmd: String,
      extension: String,
      inputStream: ByteArrayInputStream
  ): Either[Throwable, PrinterOutput] = {
    val process = cmd #< inputStream
    var output  = ""
    val logger  = ProcessLogger(_ => (), err => output += s"$err\n")
    Try(process !! logger) match {
      case Failure(e) => Left(new Exception(output, e))
      case Success(c) => Right(PrinterOutput.Text(c, extension, output))
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
    val process  = new File(filename) #< cmd #< inputStream
    var output   = ""
    val logger   = ProcessLogger(_ => (), err => output += s"$err\n")
    Try(process ! logger) match {
      case Failure(e) => Left(new Exception(output, e))
      case Success(_) => Right(PrinterOutput.File(filename, output))
    }
  }
}
