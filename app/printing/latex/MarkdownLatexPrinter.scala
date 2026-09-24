package printing.latex

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import scala.sys.process.*
import scala.util.control.NonFatal

final class MarkdownLatexPrinter(texCmd: String) {

  def toLatex(input: String, headingShift: Int = 0): Either[(Throwable, String), String] = {
    val inputStream = toStream(input)
    // replaces backslashes with the appropriate \textbackslash{} command
    val sedCmd  = "sed s/\\\\/\\\\textbackslash{}/g"
    val command = if headingShift == 0 then texCmd else s"$texCmd --shift-heading-level-by=$headingShift"
    val process = sedCmd #| command #< inputStream
    val sdtErr  = new StringBuilder()
    val logger  = ProcessLogger(_ => {}, sdtErr.append)
    try {
      Right(process !! logger)
    } catch {
      case NonFatal(e) =>
        Left(e, sdtErr.toString())
    }
  }

  private def toStream(input: String) =
    new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))
}
