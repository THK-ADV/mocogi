package controllers

import controllers.ModuleCompendiumParsingController.{
  mcgErrorWrites,
  moduleCompendiumFormat,
  parsingErrorWrites
}
import controllers.json._
import controllers.parameter.{OutputType, PrinterOutputFormat}
import parser.ParsingError
import parserprinter.ModuleCompendiumParserPrinter
import parsing.ModuleCompendiumParser
import parsing.types.ModuleRelation.{Child, Parent}
import parsing.types._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import printer.PrintingError
import printing.{ModuleCompendiumGenerationError, PrinterOutput}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

@Singleton
class ModuleCompendiumParsingController @Inject() (
    cc: ControllerComponents,
    ws: WSClient,
    parser: ModuleCompendiumParser,
    parserPrinter: ModuleCompendiumParserPrinter,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with RequestBodyFileParser {

  def generateFromFile() =
    Action(parse.temporaryFile).async { implicit r =>
      for {
        input <- Future.fromTry(parseFileContent)
        outputType <- Future.fromTry(parseOutputType)
        outputFormat <- Future.fromTry(parsePrinterOutputFormat)
        result <- render(input, outputType, outputFormat)
      } yield result
    }

  def generateFromUrl() =
    Action.async { implicit r =>
      for {
        url <- Future.fromTry(parseInputUrl)
        outputType <- Future.fromTry(parseOutputType)
        outputFormat <- Future.fromTry(parsePrinterOutputFormat)
        output <- ws
          .url(url)
          .get()
          .flatMap(r =>
            render(r.bodyAsBytes.utf8String, outputType, outputFormat)
          )
      } yield output
    }

  private def parseInputUrl(implicit r: Request[_]): Try[String] =
    getQueryString("input-url")

  private def parseOutputType(implicit r: Request[_]): Try[OutputType] =
    getQueryString("output-type").flatMap(OutputType.apply)

  private def parsePrinterOutputFormat(implicit
      r: Request[_]
  ): Try[PrinterOutputFormat] =
    getQueryString("printer-output-format").flatMap(PrinterOutputFormat.apply)

  private def getQueryString(key: String)(implicit r: Request[_]): Try[String] =
    r.getQueryString(key) match {
      case Some(value) => Success(value)
      case None => Failure(new Throwable(s"expected query parameter $key"))
    }

  private def render(
      input: String,
      outputType: OutputType,
      printerOutputFormat: PrinterOutputFormat
  ): Future[Result] =
    outputType match {
      case OutputType.JSON =>
        parser
          .parser()
          .map(_.parse(input)._1 match {
            case Right(c) =>
              Ok(Json.toJson(c))
            case Left(e) =>
              InternalServerError(Json.toJson(e))
          })
      case OutputType.Printer(printerOutputType) =>
        parserPrinter
          .print(
            input,
            printerOutputType,
            printerOutputFormat
          )
          .map {
            case Right(output) =>
              output match {
                case PrinterOutput.File(file, filename) =>
                  Ok.sendFile(
                    content = file,
                    fileName = _ => Some(filename)
                  )
                case PrinterOutput.Text(content, _) =>
                  Ok(content)
              }
            case Left(e) =>
              InternalServerError(Json.toJson(e))
          }
    }
}

object ModuleCompendiumParsingController
    extends JsonNullWritable
    with ThrowableWrites
    with LocationFormat
    with LanguageFormat
    with StatusFormat
    with AssessmentMethodFormat
    with ModuleTypeFormat
    with SeasonFormat
    with PersonFormat
    with MetadataFormat {

  implicit val mcgErrorWrites: Writes[ModuleCompendiumGenerationError] =
    Writes.apply {
      case ModuleCompendiumGenerationError.Parsing(e)  => Json.toJson(e)
      case ModuleCompendiumGenerationError.Printing(e) => Json.toJson(e)
      case ModuleCompendiumGenerationError.Other(e)    => Json.toJson(e)
    }

  implicit val parsingErrorWrites: Writes[ParsingError] =
    Writes.apply(e =>
      Json.obj(
        "type" -> "parsing error",
        "expected" -> e.expected,
        "actual" -> e.found
      )
    )

  implicit val printingErrorWrites: Writes[PrintingError] =
    Writes.apply(e =>
      Json.obj(
        "type" -> "printing error",
        "expected" -> e.expected,
        "actual" -> e.actual
      )
    )

  implicit val contentFormat: Format[Content] =
    Json.format[Content]

  implicit val moduleCompendiumFormat: Format[ModuleCompendium] =
    Json.format[ModuleCompendium]
}
