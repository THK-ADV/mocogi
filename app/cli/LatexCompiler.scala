package cli

import java.nio.file.Path

import scala.sys.process.*
import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import scala.util.control.NonFatal

object LatexCompiler {
  def getPdf(file: Path): Either[String, Path] =
    try {
      Right(
        file.resolveSibling(
          file.getFileName.toString.replace(".tex", ".pdf")
        )
      )
    } catch {
      case NonFatal(e) => Left(e.getMessage)
    }

  def compile(file: Path): Either[String, String] = {
    val process = Process(
      command = s"latexmk -xelatex -halt-on-error ${file.getFileName.toString}",
      cwd = file.getParent.toAbsolutePath.toFile
    )
    exec(process)
  }

  private def exec(process: ProcessBuilder): Either[String, String] = {
    val builder = new StringBuilder()
    val pLogger =
      ProcessLogger(
        a => builder.append(s"$a\n"),
        a => builder.append(s"${Console.RED}$a${Console.RESET}\n")
      )
    try {
      val res = process ! pLogger
      Either.cond(
        res == 0,
        builder.toString(),
        builder.toString()
      )
    } catch {
      case NonFatal(e) => Left(e.getMessage)
    }
  }
}
