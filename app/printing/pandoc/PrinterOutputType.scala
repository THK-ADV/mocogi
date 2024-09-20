package printing.pandoc

sealed trait PrinterOutputType

object PrinterOutputType {
  case object HTML           extends PrinterOutputType
  case object HTMLStandalone extends PrinterOutputType
  case class HTMLFile(
      deOutputFolderPath: String,
      enOutputFolderPath: String
  ) extends PrinterOutputType
  case class HTMLStandaloneFile(
      deOutputFolderPath: String,
      enOutputFolderPath: String
  ) extends PrinterOutputType
  case class PDFFile(
      deOutputFolderPath: String,
      enOutputFolderPath: String
  ) extends PrinterOutputType
  case class PDFStandaloneFile(
      deOutputFolderPath: String,
      enOutputFolderPath: String
  ) extends PrinterOutputType
}
