package git

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class GitReviewConfig(autoApprovedLabel: String, reviewRequiredLabel: String, reviewBypassLabel: String)

object GitReviewConfig {
  def load(configuration: Configuration): GitReviewConfig =
    GitReviewConfig(
      autoApprovedLabel = configuration.nonEmptyString("git.autoApprovedLabel"),
      reviewRequiredLabel = configuration.nonEmptyString("git.reviewRequiredLabel"),
      reviewBypassLabel = configuration.nonEmptyString("git.fastForwardLabel")
    )
}
