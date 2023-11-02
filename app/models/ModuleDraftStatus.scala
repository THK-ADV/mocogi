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

  case object Waiting_For_Approval extends ModuleDraftStatus {
    override def id: String = "waiting_for_approval"
    override def deLabel: String = "Warte auf Genehmigung"
    override def enLabel: String = "Waiting for approval"
  }

  case object Waiting_For_Changes extends ModuleDraftStatus {
    override def id: String = "waiting_for_changes"
    override def deLabel: String = "Warte auf Änderungen"
    override def enLabel: String = "Waiting for changes"
  }

  case object Valid_For_Review extends ModuleDraftStatus {
    override def id: String = "valid_for_review"
    override def deLabel: String = "Bereit zum Review"
    override def enLabel: String = "Valid for review"
  }

  case object Valid_For_Publication extends ModuleDraftStatus {
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
      case "waiting_for_changes"   => Waiting_For_Changes
      case "waiting_for_approval"  => Waiting_For_Approval
      case "valid_for_review"      => Valid_For_Review
      case "valid_for_publication" => Valid_For_Publication
      case "unknown"               => Unknown
    }
}
