package models

import models.ModuleReviewRequest.Status

import java.util.UUID

case class ModuleReviewRequest(
    review: UUID,
    reviewer: String,
    status: Status
)

object ModuleReviewRequest {
  sealed trait Status {
    def id: String
    override def toString = id
  }

  object Status {
    def apply(id: String) =
      id match {
        case "approved" => Approved
        case "rejected" => Rejected
        case "pending"  => Pending
      }
  }

  case object Approved extends Status {
    override def id: String = "approved"
  }

  case object Rejected extends Status {
    override def id: String = "rejected"
  }

  case object Pending extends Status {
    override def id: String = "pending"
  }
}
