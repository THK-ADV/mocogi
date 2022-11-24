package service

import controllers.parameter.PrinterOutputFormat
import ops.EitherOps.EOps
import parsing.types.ModuleCompendium
import printing._

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}

@Singleton
final class ModuleCompendiumPrintingService @Inject() (
    private val markdownConverter: MarkdownConverter
) {

  def print(
      mc: ModuleCompendium,
      lastModified: LocalDateTime,
      outputType: PrinterOutputType,
      outputFormat: PrinterOutputFormat,
      language: PrintingLanguage
  ): Either[ModuleCompendiumGenerationError, PrinterOutput] =
    outputFormat.printer
      .printer(language, lastModified)
      .print(mc, "")
      .biFlatMap[
        ModuleCompendiumGenerationError,
        Throwable,
        PrinterOutput
      ](
        ModuleCompendiumGenerationError.Printing.apply,
        ModuleCompendiumGenerationError.Other.apply,
        markdownConverter.convert(mc.metadata.title, mc.metadata.id, outputType)
      )
}
