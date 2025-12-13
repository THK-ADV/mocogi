package printing.pandoc

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import scala.sys.process.*
import scala.util.Try

import ops.FileOps.FileOps0

final class WordLatexPrinter(cmd: String, outputFolder: String) {

  def toLatex(wordPath: Path, po: String): Try[Path] =
    Try {
      val dir             = initFolder(po)
      val dest            = dir.resolve(s"$po.tex")
      val (exitCode, out) = execCmd(dir, wordPath, dest)
      if exitCode == 0 then dest
      else {
        dir.deleteDirectory()
        throw new Exception(s"failed to covert word to latex: ${out.toString()}")
      }
    }

  private def execCmd(ctx: Path, src: Path, dest: Path): (Int, StringBuilder) = {
    val process = dest.toFile #< Process(cmd, ctx.toFile) #< src.toFile
    val out     = new StringBuilder()
    val pLogger = ProcessLogger(s => out.append(s))
    (process ! pLogger, out)
  }

  private def initFolder(filename: String): Path = {
    val dir = Paths.get(outputFolder).resolve(filename)
    if Files.isDirectory(dir) then dir.deleteDirectory()
    Files.createDirectory(dir)
  }
}
