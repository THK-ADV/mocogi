package service.notification

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class ReviewNotificationConfig(pendingReviewsUrl: String, moduleEditUrlTemplate: String)

object ReviewNotificationConfig {
  def load(configuration: Configuration): ReviewNotificationConfig =
    ReviewNotificationConfig(
      pendingReviewsUrl = configuration.nonEmptyString("mail.reviewUrl"),
      moduleEditUrlTemplate = configuration.nonEmptyString("mail.editUrl")
    )
}
