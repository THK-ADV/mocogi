package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import printing.pandoc.PandocApi

@Singleton
final class MarkdownConverterProvider @Inject() (
    config: ConfigReader
) extends Provider[PandocApi] {
  override def get() =
    new PandocApi(config.htmlCmd, config.pdfCmd, config.texCmd)
}
