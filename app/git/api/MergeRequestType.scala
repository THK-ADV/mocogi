package git.api

sealed trait MergeRequestType

object MergeRequestType {
  case object AutoAccept extends MergeRequestType
  case object NeedsApproval extends MergeRequestType
}
