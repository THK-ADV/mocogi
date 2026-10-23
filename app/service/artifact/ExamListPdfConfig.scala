package service.artifact

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class ExamListPdfConfig(tmpDir: String, publishedPdfDir: String)

object ExamListPdfConfig {
  def load(configuration: Configuration): ExamListPdfConfig =
    ExamListPdfConfig(
      tmpDir = configuration.nonEmptyString("play.temporaryFile.dir"),
      publishedPdfDir = configuration.nonEmptyString("pandoc.examListOutputFolderPath")
    )
}
