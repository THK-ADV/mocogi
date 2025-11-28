package models

import play.api.libs.json.Json
import play.api.libs.json.OWrites
import play.api.libs.json.Writes

case class UserInfo(
    hasDirectorPrivileges: Boolean,
    hasModuleReviewPrivileges: Boolean,
    hasModulesToEdit: Boolean,
    rejectedReviews: Int,
    reviewsToApprove: Int,
    fastForwardApprovalPOs: Option[Set[String]]
)

object UserInfo {
  given OWrites[UserInfo] = Json.writes
}
