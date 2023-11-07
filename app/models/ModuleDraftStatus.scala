package models

import play.api.libs.json.{Json, Writes}

sealed trait ModuleDraftStatus {
  def id: String
  def deLabel: String
  def enLabel: String
}

object ModuleDraftStatus {
  implicit val writes: Writes[ModuleDraftStatus] =
    (status: ModuleDraftStatus) =>
      Json.obj(
        "id" -> status.id,
        "deLabel" -> status.deLabel,
        "enLabel" -> status.enLabel
      )

  case object Published extends ModuleDraftStatus {
    override def id: String = "published"
    override def deLabel: String = "Veröffentlicht"
    override def enLabel: String = "Published"
  }

  case object WaitingForReview extends ModuleDraftStatus {
    override def id: String = "waiting_for_review"
    override def deLabel: String = "Warte auf Review"
    override def enLabel: String = "Waiting for review"
  }

  case object WaitingForChanges extends ModuleDraftStatus {
    override def id: String = "waiting_for_changes"
    override def deLabel: String = "Warte auf Änderungen"
    override def enLabel: String = "Waiting for changes"
  }

  case object ValidForReview extends ModuleDraftStatus {
    override def id: String = "valid_for_review"
    override def deLabel: String = "Bereit zum Review"
    override def enLabel: String = "Valid for review"
  }

  case object ValidForPublication extends ModuleDraftStatus {
    override def id: String = "valid_for_publication"
    override def deLabel: String = "Bereit zur Veröffentlichung"
    override def enLabel: String = "Valid for publication"
  }

  case object Unknown extends ModuleDraftStatus {
    override def id: String = "unknown"
    override def deLabel: String = "Unbekannt"
    override def enLabel: String = "Unknown"
  }

  def apply(id: String) =
    id match {
      case "published"             => Published
      case "waiting_for_changes"   => WaitingForChanges
      case "waiting_for_review"    => WaitingForReview
      case "valid_for_review"      => ValidForReview
      case "valid_for_publication" => ValidForPublication
      case "unknown"               => Unknown
    }
}
