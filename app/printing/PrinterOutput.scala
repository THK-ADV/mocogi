package printing

import play.api.libs.Files.TemporaryFile

sealed trait PrinterOutput

object PrinterOutput {
  case class Text(content: String, extension: String) extends PrinterOutput
  case class File(file: TemporaryFile, filename: String) extends PrinterOutput
}
