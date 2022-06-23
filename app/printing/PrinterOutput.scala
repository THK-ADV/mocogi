package printing

import play.api.libs.Files.TemporaryFile

sealed trait PrinterOutput

object PrinterOutput {
  case class HTML(content: String) extends PrinterOutput
  case class PDF(file: TemporaryFile, filename: String) extends PrinterOutput
}
