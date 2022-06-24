package providers

import play.api.libs.Files.DefaultTemporaryFileCreator
import printing.MarkdownConverter

import javax.inject.{Inject, Provider, Singleton}

@Singleton
final class MarkdownConverterProvider @Inject() (
    fileCreator: DefaultTemporaryFileCreator,
    config: ConfigReader
) extends Provider[MarkdownConverter] {
  override def get() = new MarkdownConverter(
    fileCreator,
    config.htmlCmd,
    config.pdfCmd
  )
}
