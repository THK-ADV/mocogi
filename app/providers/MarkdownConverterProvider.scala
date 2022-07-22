package providers

import printing.MarkdownConverter

import javax.inject.{Inject, Provider, Singleton}

@Singleton
final class MarkdownConverterProvider @Inject() (
    config: ConfigReader
) extends Provider[MarkdownConverter] {
  override def get() = new MarkdownConverter(
    config.htmlCmd,
    config.pdfCmd
  )
}
