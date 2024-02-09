package service

import ops.FileOps.FileOps0

import java.nio.file.Path
import scala.sys.process.{Process, ProcessLogger, _}
import scala.util.control.NonFatal

object LatexCompiler {
  def markFileAsBroken(file: Path): Either[String, Path] =
    try {
      Right(file.rename(s"BROKEN_${file.getFileName}"))
    } catch {
      case NonFatal(e) => Left(e.getMessage)
    }

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

  def clear(file: Path): Unit = {
    val process = Process(
      command = s"latexmk -c ${file.getFileName.toString}",
      cwd = file.getParent.toAbsolutePath.toFile
    )
    exec(process)
  }

  def compile(file: Path): Either[String, String] = {
    val process = Process(
      command = s"latexmk -xelatex -halt-on-error ${file.getFileName.toString}",
      cwd = file.getParent.toAbsolutePath.toFile
    )
    exec(process)
  }

  def exec(process: ProcessBuilder): Either[String, String] = {
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

  def execRes(
      process: ProcessBuilder
  )(
      statusErrMsg: PartialFunction[Int, String]
  ): Either[(String, Option[(Int, String)]), String] = {
    val builder = new StringBuilder()
    val pLogger =
      ProcessLogger(
        a => builder.append(s"$a\n"),
        a => builder.append(s"${Console.RED}$a${Console.RESET}\n")
      )
    try {
      val status = process ! pLogger
      if (status == 0) Right(builder.toString())
      else if (status < 0) Left((builder.toString(), None))
      else if (statusErrMsg.isDefinedAt(status))
        Left((builder.toString(), Some(status, statusErrMsg(status))))
      else Left((builder.toString(), None))
    } catch {
      case NonFatal(e) => Left((e.getMessage, None))
    }
  }
}
