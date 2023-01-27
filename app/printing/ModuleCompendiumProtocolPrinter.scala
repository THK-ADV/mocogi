package printing

import models.ModuleCompendiumProtocol
import parsing.metadata.VersionScheme
import printer.Printer
import printer.Printer.newline

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
final class ModuleCompendiumProtocolPrinter @Inject() (
    private val metadataProtocolPrinter: MetadataProtocolPrinter,
    private val contentPrinter: ContentPrinter
) {
  def printer(
      version: VersionScheme
  ): Printer[(UUID, ModuleCompendiumProtocol)] =
    metadataProtocolPrinter
      .printer(version)
      .skip(newline)
      .zip(contentPrinter.printer())
      .contraMapSuccess(a =>
        ((a._1, a._2.metadata), (a._2.deContent, a._2.enContent))
      )
}
