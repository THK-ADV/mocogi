package models

sealed trait ModuleReviewStatus {
  def id: String
}

object ModuleReviewStatus {
  case object Approved extends ModuleReviewStatus {
    override val id: String = "approved"
  }

  case object Rejected extends ModuleReviewStatus {
    override val id: String = "rejected"
  }

  case object Pending extends ModuleReviewStatus {
    override val id: String = "pending"
  }

  def apply(id: String): ModuleReviewStatus =
    id match {
      case "approved" => Approved
      case "rejected" => Rejected
      case "pending"  => Pending
    }
}
