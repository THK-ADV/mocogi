package printing.pandoc

sealed trait PrinterOutputType

object PrinterOutputType {
  case object HTML                                        extends PrinterOutputType
  case object HTMLStandalone                              extends PrinterOutputType
  case class HTMLFile(outputFolderPath: String)           extends PrinterOutputType
  case class HTMLStandaloneFile(outputFolderPath: String) extends PrinterOutputType
  case class PDFFile(outputFolderPath: String)            extends PrinterOutputType
  case class PDFStandaloneFile(outputFolderPath: String)  extends PrinterOutputType
}
