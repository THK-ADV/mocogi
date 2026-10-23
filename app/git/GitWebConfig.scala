package git

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class GitWebConfig(repoUrl: String)

object GitWebConfig {
  def load(configuration: Configuration): GitWebConfig = GitWebConfig(configuration.nonEmptyString("git.repoUrl"))
}
