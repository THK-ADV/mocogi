package service.artifact

import java.nio.file.Files
import java.nio.file.Path

import scala.util.control.NonFatal

opaque type GeneratedPdf = Path

object GeneratedPdf {
  def apply(path: Path): GeneratedPdf = path

  extension (pdf: GeneratedPdf) {
    def path: Path = pdf

    def moveTo(destination: Path, filename: String): Either[String, GeneratedPdf] =
      try Right(GeneratedPdf(Files.move(pdf, destination.resolve(filename))))
      catch case NonFatal(error) => Left(error.getMessage)

    def rename(filename: String): Either[String, GeneratedPdf] =
      moveTo(pdf.getParent, filename)
  }
}
