package parserprinter

import controllers.parameter.PrinterOutputFormat
import ops.EitherOps.EOps
import parsing.ModuleCompendiumParser
import parsing.types.ModuleCompendium
import printing._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ModuleCompendiumParserPrinter @Inject() (
    private val parser: ModuleCompendiumParser,
    private val printer: ModuleCompendiumPrinter,
    private val markdownConverter: MarkdownConverter,
    private implicit val ctx: ExecutionContext
) {

  def print(
      mc: ModuleCompendium,
      outputType: PrinterOutputType,
      outputFormat: PrinterOutputFormat
  ): Either[ModuleCompendiumGenerationError, PrinterOutput] =
    printer
      .printerForFormat(outputFormat)
      .print(mc, "")
      .map(_ -> mc.metadata.id)
      .biFlatMap[
        ModuleCompendiumGenerationError,
        Throwable,
        PrinterOutput
      ](
        ModuleCompendiumGenerationError.Printing.apply,
        ModuleCompendiumGenerationError.Other.apply,
        a => markdownConverter.convert(a._2, a._1, outputType)
      )

  def print(
      input: String,
      outputType: PrinterOutputType,
      outputFormat: PrinterOutputFormat
  ): Future[Either[ModuleCompendiumGenerationError, PrinterOutput]] =
    parser
      .parser()
      .map(
        _.parse(input)._1
          .biFlatMap[
            ModuleCompendiumGenerationError,
            ModuleCompendiumGenerationError,
            PrinterOutput
          ](
            ModuleCompendiumGenerationError.Parsing.apply,
            identity,
            mc => print(mc, outputType, outputFormat)
          )
      )

}
