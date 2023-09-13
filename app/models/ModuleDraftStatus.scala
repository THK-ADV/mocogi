package models

import play.api.libs.json.Format

sealed trait ModuleDraftStatus {
  def id: String
}

object ModuleDraftStatus {
  implicit val format: Format[ModuleDraftStatus] =
    Format.of[String].bimap(ModuleDraftStatus.apply, _.id)

  case object Published extends ModuleDraftStatus {
    override val id: String = "published"
  }

  case object Waiting_For_Approval extends ModuleDraftStatus {
    override val id: String = "waiting_for_approval"
  }

  case object Waiting_For_Publication extends ModuleDraftStatus {
    override val id: String = "waiting_for_publication"
  }

  case object Valid_For_Review extends ModuleDraftStatus {
    override val id: String = "valid_for_review"
  }

  case object Valid_For_Publication extends ModuleDraftStatus {
    override val id: String = "valid_for_publication"
  }

  case object Unknown extends ModuleDraftStatus {
    override val id: String = "unknown"
  }

  def apply(id: String) =
    id match {
      case "published"               => Published
      case "waiting_for_approval"    => Waiting_For_Approval
      case "waiting_for_publication" => Waiting_For_Publication
      case "valid_for_review"        => Valid_For_Review
      case "valid_for_publication"   => Valid_For_Publication
      case "unknown"                 => Unknown
    }
}
