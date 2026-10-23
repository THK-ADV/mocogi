package git.api

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration
import settings.SecretString

final case class GitApiConfig(accessToken: SecretString, baseUrl: String, projectId: Int)

object GitApiConfig {
  def load(configuration: Configuration): GitApiConfig =
    GitApiConfig(
      accessToken = SecretString.unsafe(configuration.nonEmptyString("git.accessToken")),
      baseUrl = configuration.nonEmptyString("git.baseUrl"),
      projectId = configuration.get[Int]("git.projectId")
    )
}
