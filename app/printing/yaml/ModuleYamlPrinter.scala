package printing.yaml

import models.ModuleProtocol
import parsing.metadata.VersionScheme
import printer.Printer.newline
import printer.{Printer, PrintingError}

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
final class ModuleYamlPrinter @Inject() (
    private val metadataProtocolPrinter: MetadataYamlPrinter,
    private val contentPrinter: ContentMarkdownPrinter
) {

  def print(
      version: VersionScheme,
      moduleId: UUID,
      protocol: ModuleProtocol
  ): Either[PrintingError, String] =
    printer(version)
      .print((moduleId, protocol), new StringBuilder())
      .map(_.toString())

  private def printer(
      version: VersionScheme
  ): Printer[(UUID, ModuleProtocol)] =
    metadataProtocolPrinter
      .printer(version)
      .skip(newline)
      .zip(contentPrinter.printer())
      .contraMapSuccess(a =>
        ((a._1, a._2.metadata), (a._2.deContent, a._2.enContent))
      )
}
