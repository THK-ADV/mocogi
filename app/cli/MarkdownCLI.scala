package cli

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.UUID

import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import scala.util.control.NonFatal

import ops.FileOps.deleteDirectory
import settings.MarkdownSettings

final class MarkdownCLI(settings: MarkdownSettings) {

  private val MaxFormatPasses = 5
  private val command         = Seq("markdownlint-cli2", "--config", settings.configPath)
  private val lintIssue       = """^(?:.*[/\\])?(\d+\.md):(.*)$""".r

  def format(input: String): Either[String, String] =
    format(input, MaxFormatPasses).flatMap(formatted => lint(formatted).map(_ => formatted))

  def lint(input: String): Either[String, Unit] =
    run(Some(input), "-").map(_ => ())

  def lint(inputs: Seq[(Option[UUID], String)]): Either[String, Seq[(Option[UUID], String)]] =
    if inputs.isEmpty then Right(Seq.empty)
    else
      try {
        val folder = Files.createTempDirectory("mocogi-markdown-")
        try {
          val files = inputs.zipWithIndex.map {
            case ((moduleId, input), index) =>
              val name = s"$index.md"
              Files.writeString(folder.resolve(name), input, UTF_8)
              name -> moduleId
          }
          run(None, folder.resolve("*.md").toString) match {
            case Right(_)     => Right(Seq.empty)
            case Left(output) => parseIssues(output, files)
          }
        } finally folder.deleteDirectory()
      } catch {
        case NonFatal(exception) => Left(exception.getMessage)
      }

  private def format(input: String, remainingPasses: Int): Either[String, String] =
    run(Some(input), "--format", "-").flatMap { formatted =>
      if formatted == input || remainingPasses == 1 then Right(formatted)
      else format(formatted, remainingPasses - 1)
    }

  private def parseIssues(
      output: String,
      files: Seq[(String, Option[UUID])]
  ): Either[String, Seq[(Option[UUID], String)]] = {
    val lines  = output.linesIterator.toSeq
    val issues = lines.collect { case lintIssue(file, issue) => file -> issue }

    if issues.size != lines.size then Left(output)
    else {
      val byFile = issues.groupMap(_._1)(_._2)
      Right(files.flatMap { (file, moduleId) =>
        byFile.get(file).map(issues => moduleId -> issues.mkString("\n"))
      })
    }
  }

  private def run(input: Option[String], arguments: String*): Either[String, String] = {
    val error   = new StringBuilder()
    val logger  = ProcessLogger(_ => (), line => error.append(s"$line\n"))
    val process = input.fold(Process(command ++ arguments)) { input =>
      Process(command ++ arguments) #< new ByteArrayInputStream(input.getBytes(UTF_8))
    }

    try Right(process.!!(logger))
    catch {
      case NonFatal(exception) =>
        Left(if error.nonEmpty then error.toString().trim else exception.getMessage)
    }
  }
}
