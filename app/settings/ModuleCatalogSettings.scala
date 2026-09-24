package settings

final case class ModuleCatalogSettings(
    tmpDir: String,
    publishedPdfDir: String,
    introDir: String,
    assetsDir: String,
    texCommand: String,
    wordCommand: String
)
