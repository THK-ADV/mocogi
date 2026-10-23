package service.artifact

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class ExamLoadConfig(tmpDir: String)

object ExamLoadConfig {
  def load(configuration: Configuration): ExamLoadConfig =
    ExamLoadConfig(configuration.nonEmptyString("play.temporaryFile.dir"))
}
