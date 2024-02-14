package models

import models.ModuleDraftState.{ValidForPublication, ValidForReview}
import models.core.IDLabel
import play.api.libs.json.Writes

sealed trait ModuleDraftState extends IDLabel {
  def canRequestReview: Boolean =
    this == ValidForReview || this == ValidForPublication

  def canEdit: Boolean = this match {
    case ModuleDraftState.Published | ModuleDraftState.ValidForReview |
        ModuleDraftState.ValidForPublication |
        ModuleDraftState.WaitingForChanges =>
      true
    case ModuleDraftState.WaitingForReview | ModuleDraftState.Unknown |
        ModuleDraftState.WaitingForPublication =>
      false
  }
}

object ModuleDraftState {

  implicit def writes: Writes[ModuleDraftState] =
    Writes.of[IDLabel].contramap(identity)

  case object Published extends ModuleDraftState {
    override def id: String = "published"
    override def deLabel: String = "Veröffentlicht"
    override def enLabel: String = "Published"
  }

  case object ValidForReview extends ModuleDraftState {
    override def id: String = "valid_for_review"
    override def deLabel: String = "Bereit zum Review"
    override def enLabel: String = "Valid for review"
  }

  case object ValidForPublication extends ModuleDraftState {
    override def id: String = "valid_for_publication"
    override def deLabel: String = "Bereit zur Veröffentlichung"
    override def enLabel: String = "Valid for publication"
  }

  case object WaitingForChanges extends ModuleDraftState {
    override def id: String = "waiting_for_changes"
    override def deLabel: String = "Warte auf Änderungen"
    override def enLabel: String = "Waiting for changes"
  }

  case object WaitingForReview extends ModuleDraftState {
    override def id: String = "waiting_for_review"
    override def deLabel: String = "Warte auf Review"
    override def enLabel: String = "Waiting for review"
  }

  case object WaitingForPublication extends ModuleDraftState {
    override def id: String = "waiting_for_publication"
    override def deLabel: String = "Warte auf Veröffentlichung"
    override def enLabel: String = "Waiting for publication"
  }

  case object Unknown extends ModuleDraftState {
    override def id: String = "unknown"
    override def deLabel: String = "Unbekannt"
    override def enLabel: String = "Unknown"
  }
}
