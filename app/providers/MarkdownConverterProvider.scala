package providers

import printing.pandoc.PandocApi

import javax.inject.{Inject, Provider, Singleton}

@Singleton
final class MarkdownConverterProvider @Inject() (
    config: ConfigReader
) extends Provider[PandocApi] {
  override def get() = new PandocApi(config.htmlCmd, config.pdfCmd)
}
