package models

sealed trait ModuleReviewStatus {
  def id: String
}

object ModuleReviewStatus {
  case object WaitingForApproval extends ModuleReviewStatus {
    override val id: String = "waiting_for_approval"
  }
  case object WaitingForChanges extends ModuleReviewStatus {
    override val id: String = "waiting_for_changes"
  }
  case object Approved extends ModuleReviewStatus {
    override val id: String = "approved"
  }

  def apply(id: String): ModuleReviewStatus =
    id match {
      case "waiting_for_approval" => WaitingForApproval
      case "waiting_for_changes"  => WaitingForChanges
      case "approved"             => Approved
    }
}
