package git.api

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class GitCommitConfig(fallbackAuthorEmail: String)

object GitCommitConfig {
  def load(configuration: Configuration): GitCommitConfig =
    GitCommitConfig(configuration.nonEmptyString("git.defaultEmail"))
}
