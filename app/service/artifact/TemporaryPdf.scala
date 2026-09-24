package service.artifact

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.Failure

import ops.FileOps
import ops.FileOps.deleteDirectory
import service.artifact.GeneratedPdf.*

/** A generated PDF in its own working directory, ready for preview or publication. */
final class TemporaryPdf private[artifact] (val path: Path) extends AutoCloseable {

  /** Deletes the working directory and its contents; close previews only after delivery finishes. */
  override def close(): Unit = path.getParent.deleteDirectory()

  /** Moves the PDF to a unique timestamped filename and returns that name; cleans up even if publication fails. */
  def publish(filenamePrefix: String, destination: String): String =
    try {
      val timestamp = TemporaryPdf.publishedFilenameTimestamp.format(Instant.now())
      val filename  = s"${filenamePrefix}_${timestamp}_${UUID.randomUUID()}.pdf"
      Files.move(path, Paths.get(destination).resolve(filename)).getFileName.toString
    } finally close()
}

object TemporaryPdf {
  private def publishedFilenameTimestamp =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

  /** Generates a PDF in its own directory and cleans up on failure; callers must close or publish successful results. */
  def generate(filename: String, tmpDir: String)(
      create: Path => Future[GeneratedPdf]
  )(using ctx: ExecutionContext): Future[TemporaryPdf] = {
    val latexFile = FileOps.createLatexFile(filename, tmpDir)
    val result    =
      try create(latexFile)
      catch { case NonFatal(error) => Future.failed(error) }
    result
      .map(pdf => new TemporaryPdf(pdf.path))
      .andThen { case Failure(_) => latexFile.getParent.deleteDirectory() }
  }
}
