package models

import models.core.IDLabel
import play.api.libs.json.Writes

sealed trait ModuleReviewStatus extends IDLabel {
  def isRejected = this == ModuleReviewStatus.Rejected
}

object ModuleReviewStatus {

  implicit def writes: Writes[ModuleReviewStatus] =
    Writes.of[IDLabel].contramap(identity)

  case object Approved extends ModuleReviewStatus {
    override def id: String = "approved"
    override def deLabel    = "Genehmigt"
    override def enLabel    = "Approved"
  }

  case object Rejected extends ModuleReviewStatus {
    override def id: String = "rejected"
    override def deLabel    = "Abgelehnt"
    override def enLabel    = "Rejected"
  }

  case object Pending extends ModuleReviewStatus {
    override def id: String = "pending"
    override def deLabel    = "Ausstehend"
    override def enLabel    = "Pending"
  }

  def apply(id: String): ModuleReviewStatus =
    id match {
      case "approved" => Approved
      case "rejected" => Rejected
      case "pending"  => Pending
    }
}
