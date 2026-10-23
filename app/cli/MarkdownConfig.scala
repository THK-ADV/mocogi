package cli

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class MarkdownConfig(lintConfigPath: String)

object MarkdownConfig {
  def load(configuration: Configuration): MarkdownConfig =
    MarkdownConfig(configuration.nonEmptyString("markdown.configPath"))
}
