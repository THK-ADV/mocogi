package service.artifact.modulecatalog

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class ModuleCatalogPdfConfig(
    tmpDir: String,
    publishedPdfDir: String,
    introDir: String,
    assetsDir: String,
    markdownToLatexCommand: String,
    docxToLatexCommand: String
)

object ModuleCatalogPdfConfig {
  def load(configuration: Configuration): ModuleCatalogPdfConfig =
    ModuleCatalogPdfConfig(
      tmpDir = configuration.nonEmptyString("play.temporaryFile.dir"),
      publishedPdfDir = configuration.nonEmptyString("pandoc.moduleCatalogOutputFolderPath"),
      introDir = configuration.nonEmptyString("pandoc.mcIntroPath"),
      assetsDir = configuration.nonEmptyString("pandoc.mcAssetsPath"),
      markdownToLatexCommand = configuration.nonEmptyString("pandoc.texCmd"),
      docxToLatexCommand = configuration.nonEmptyString("pandoc.wordCmd")
    )
}
