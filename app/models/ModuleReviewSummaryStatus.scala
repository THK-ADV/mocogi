package models

import play.api.libs.json.{Json, Writes}

sealed trait ModuleReviewSummaryStatus {
  def id: String
  def deLabel: String
  def enLabel: String
}

object ModuleReviewSummaryStatus {
  implicit def writes: Writes[ModuleReviewSummaryStatus] = {
    case s @ WaitingForChanges =>
      Json.obj(
        "id" -> s.id,
        "deLabel" -> s.deLabel,
        "enLabel" -> s.enLabel
      )
    case s @ WaitingForReview(approved, needed) =>
      Json.obj(
        "id" -> s.id,
        "deLabel" -> s.deLabel,
        "enLabel" -> s.enLabel,
        "approved" -> approved,
        "needed" -> needed
      )
  }

  case object WaitingForChanges extends ModuleReviewSummaryStatus {
    override def id: String = "waiting_for_changes"

    override def deLabel: String = "Warte auf Änderungen"

    override def enLabel: String = "Waiting for changes"
  }

  case class WaitingForReview(approved: Int, needed: Int)
      extends ModuleReviewSummaryStatus {
    override def id: String = "waiting_for_review"

    override def deLabel: String = s"Warte auf Review ($approved/$needed)"

    override def enLabel: String = s"Waiting for review ($approved/$needed)"
  }
}
