package models

import play.api.libs.json.{Json, Writes}

sealed trait ModuleReviewStatus {
  def id: String
  def deLabel: String
  def enLabel: String
}

object ModuleReviewStatus {
  case object Approved extends ModuleReviewStatus {
    override def id: String = "approved"
    override def deLabel = "Genehmigt"
    override def enLabel = "Approved"
  }

  case object Rejected extends ModuleReviewStatus {
    override def id: String = "rejected"
    override def deLabel = "Abgelehnt"
    override def enLabel = "Rejected"
  }

  case object Pending extends ModuleReviewStatus {
    override def id: String = "pending"
    override def deLabel = "Ausstehend"
    override def enLabel = "Pending"
  }

  def apply(id: String): ModuleReviewStatus =
    id match {
      case "approved" => Approved
      case "rejected" => Rejected
      case "pending"  => Pending
    }

  implicit def writes: Writes[ModuleReviewStatus] =
    s =>
      Json.obj(
        "id" -> s.id,
        "deLabel" -> s.deLabel,
        "enLabel" -> s.enLabel
      )
}
