package printing

sealed trait PrinterOutput

object PrinterOutput {
  case class Text(content: String, extension: String) extends PrinterOutput
  case class File(path: String) extends PrinterOutput
}
