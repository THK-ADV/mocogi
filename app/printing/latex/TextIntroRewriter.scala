package printing.latex

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

import scala.util.Try

import ops.FileOps.rename

final class TextIntroRewriter {

  private val prefix = "intro_"

  def rewrite(texFile: Path): Try[Path] =
    Try(rename(rewriteTexFile(texFile)))

  private def rename(path: Path): Path =
    path.rename(prefix + path.getFileName.toString)

  /** Force figures to render at their defined position */
  private def figureFloat: PartialFunction[String, String] = {
    case line if line.startsWith("\\begin{figure}") => "\\begin{figure}[H]"
  }

  /** Normalize image width and reject unsupported .emf files */
  private def fixImage: PartialFunction[String, String] = {
    case line if line.startsWith("\\includegraphics") =>
      if line.contains(".emf") then "\\textbf{unable to include .emf image file}"
      else {
        val img = line.dropWhile(_ != ']')
        s"\\includegraphics[width=1.0\\textwidth$img"
      }
  }

  /** Remove redundant "Abbildung"/"Tabelle" prefix from captions */
  private def stripCaptionPrefix: PartialFunction[String, String] = {
    case line if line.startsWith("\\caption{") =>
      line.replaceFirst(
        """\\caption\{(Abbildung|Tabelle)\s*:?\s*\d+(?:\.\d+)*\s*:?\s*""",
        "\\\\caption{"
      )
  }

  /** Pass through unmatched lines unchanged */
  private def identity: PartialFunction[String, String] = { case line => line }

  private def rewriteLine: String => String =
    figureFloat.orElse(fixImage).orElse(stripCaptionPrefix).orElse(identity)

  private def rewriteTexFile(path: Path): Path = {
    val rewrite = Files
      .lines(path)
      .map(rewriteLine(_))
      .collect(Collectors.joining("\n"))
    Files.writeString(path, rewrite)
  }
}
