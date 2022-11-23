package printing

sealed trait PrinterOutput {
  def consoleOutput: String
}

object PrinterOutput {
  case class Text(content: String, extension: String, consoleOutput: String)
      extends PrinterOutput
  case class File(path: String, consoleOutput: String) extends PrinterOutput
}
