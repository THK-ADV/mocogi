package printing.latex

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

import scala.util.Try

import ops.FileOps.FileOps0

final class TextIntroRewriter {

  private val prefix = "intro_"

  def rewrite(texFile: Path): Try[Path] =
    Try(rename(rewriteTexFile(texFile)))

  private def rename(path: Path): Path =
    path.rename(prefix + path.getFileName.toString)

  private def rewriteTexFile(path: Path): Path = {
    val rewrite = Files
      .lines(path)
      .map(line =>
        if !line.startsWith("\\includegraphics") then line
        else {
          if line.contains(".emf") then "\\textbf{unable to include .emf image file}"
          else {
            val img = line.dropWhile(_ != ']')
            s"\\includegraphics[width=1.0\\textwidth$img"
          }
        }
      )
      .collect(Collectors.joining("\n"))
    Files.writeString(path, rewrite)
  }
}
