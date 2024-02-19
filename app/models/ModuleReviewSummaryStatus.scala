package models

import models.core.IDLabel
import play.api.libs.json.{Json, Writes}

sealed trait ModuleReviewSummaryStatus extends IDLabel

object ModuleReviewSummaryStatus {
  implicit def writes: Writes[ModuleReviewSummaryStatus] = {
    case s @ WaitingForChanges => IDLabel.writes.writes(s)
    case s @ WaitingForReview(approved, needed) =>
      IDLabel.writes
        .writes(s)
        .++(Json.obj("approved" -> approved, "needed" -> needed))
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
