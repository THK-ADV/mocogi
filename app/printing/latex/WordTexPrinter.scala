package printing.latex

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import scala.sys.process.*
import scala.util.Try

import models.FullPoId
import ops.FileOps.FileOps0
import play.api.Logging

final class WordTexPrinter(cmd: String, outputFolder: String) extends Logging {

  def toTex(wordPath: Path, fullPoId: FullPoId): Try[Path] =
    Try {
      isWordFile(wordPath)
      val dir              = createFolder(fullPoId.id)
      val dest             = dir.resolve(s"${fullPoId.id}.tex")
      val (code, out, err) = execCmd(dir, wordPath, dest)
      if (out.nonEmpty) {
        logger.info(out.toString())
      }
      if (err.nonEmpty) {
        logger.error(err.toString())
      }
      logger.info(s"printed $wordPath to tex file $dest. Return Code: $code")
      dest
    }

  private def isWordFile(path: Path): Unit =
    Files.probeContentType(path) match
      case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" =>
        ()
      case null =>
        throw new Exception("expected word file, but no content type was found")
      case other =>
        throw new Exception(s"expected word file, but was: $other")

  private def execCmd(ctx: Path, src: Path, dest: Path) = {
    val process = dest.toFile #< Process(cmd, ctx.toFile) #< src.toFile
    val out     = new StringBuilder()
    val err     = new StringBuilder()
    val pLogger = ProcessLogger(fout => out.append(s"$fout\n"), ferr => err.append(s"$ferr\n"))
    (process ! pLogger, out, err)
  }

  private def createFolder(filename: String): Path = {
    val dir = Paths.get(outputFolder).resolve(filename)
    if Files.isDirectory(dir) then dir.deleteDirectory()
    Files.createDirectory(dir)
  }
}
