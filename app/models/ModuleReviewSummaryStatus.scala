package models

import models.core.IDLabel
import models.ModuleReviewStatus.Approved
import models.ModuleReviewStatus.Pending
import models.ModuleReviewStatus.Rejected
import monocle.syntax.all.*
import play.api.libs.json.Json
import play.api.libs.json.Writes

sealed trait ModuleReviewSummaryStatus extends IDLabel

object ModuleReviewSummaryStatus {
  given Writes[ModuleReviewSummaryStatus] = {
    case s @ WaitingForChanges                  => IDLabel.writes.writes(s)
    case s @ WaitingForReview(approved, needed) =>
      IDLabel.writes
        .writes(s)
        .++(Json.obj("approved" -> approved, "needed" -> needed))
  }

  def apply(reviews: Seq[ModuleReviewStatus]): Option[ModuleReviewSummaryStatus] =
    Option.when(reviews.nonEmpty) {
      val (approved, rejected) =
        reviews.foldLeft((0, 0)) {
          case (acc, s) =>
            s match {
              case Approved =>
                acc.focus(_._1).modify(_ + 1)
              case Rejected =>
                acc.focus(_._2).modify(_ + 1)
              case Pending => acc
            }
        }
      if (rejected > 0) WaitingForChanges
      else WaitingForReview(approved, reviews.size)
    }

  case object WaitingForChanges extends ModuleReviewSummaryStatus {
    override def id: String = "waiting_for_changes"

    override def deLabel: String = "Warte auf Änderungen"

    override def enLabel: String = "Waiting for changes"
  }

  case class WaitingForReview(approved: Int, needed: Int) extends ModuleReviewSummaryStatus {
    override def id: String = "waiting_for_review"

    override def deLabel: String = s"Warte auf Review ($approved/$needed)"

    override def enLabel: String = s"Waiting for review ($approved/$needed)"
  }
}
