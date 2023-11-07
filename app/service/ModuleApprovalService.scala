package service

import com.google.inject.{Inject, Singleton}
import controllers.formats.ModuleCompendiumProtocolFormat
import database.repo.ModuleApprovalRepository
import models.ModuleReviewStatus.{Approved, Pending, Rejected}
import models.{ModuleReviewStatus, UniversityRole, User}
import monocle.Monocle.toAppliedFocusOps
import play.api.libs.json.Json
import service.ModuleApprovalService.ModuleReviewSummaryStatus.{WaitingForChanges, WaitingForReview}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object ModuleApprovalService {
  sealed trait ModuleReviewSummaryStatus {
    def id: String

    def deLabel: String

    def enLabel: String
  }

  object ModuleReviewSummaryStatus {
    case object WaitingForChanges extends ModuleReviewSummaryStatus {
      override def id: String = "waiting_for_changes"
      override def deLabel: String = "Warte auf Änderungen"
      override def enLabel: String = "Waiting for changes"
    }

    case class WaitingForReview(approved: Int, needed: Int)
        extends ModuleReviewSummaryStatus {
      override def id: String = "waiting_for_review"
      override def deLabel: String = s"Warte auf Review"
      override def enLabel: String = s"Waiting for review"
    }
  }

  case class ReviewerApproval(
      reviewId: UUID,
      moduleId: UUID,
      moduleTitle: String,
      moduleAbbrev: String,
      author: User,
      role: UniversityRole,
      status: ModuleReviewSummaryStatus,
      studyProgram: String,
      canReview: Boolean
  )
}

@Singleton
final class ModuleApprovalService @Inject() (
    private val approvalRepository: ModuleApprovalRepository,
    private implicit val ctx: ExecutionContext
) extends ModuleCompendiumProtocolFormat {

  import ModuleApprovalService._

  /** Returns the ModuleReviewSummaryStatus for the given module
    * @param moduleId
    *   ID of the module
    * @return
    *   Some Waiting For Changes or Waiting For Review with progress indicator
    *   if a review does exists for the module. None otherwise.
    */
  def summaryStatus(moduleId: UUID): Future[Option[ModuleReviewSummaryStatus]] =
    approvalRepository.getAllStatus(moduleId).map(summaryStatus0)

  /** Returns all reviews with a corresponding review status and whether the
    * given user (reviewer) can perform a review. A review status is defined as
    * waiting for review (with a progress indicator) or waiting for changes.
    *
    * @param user
    *   The user which requested the reviews
    * @return
    */
  def reviewerApprovals(user: User): Future[Iterable[ReviewerApproval]] = {
    approvalRepository
      .allByModulesWhereUserExists(user)
      .map(_.groupBy(_._1).flatMap { case (_, entries) =>
        entries.filter(_._7.isDefined).map {
          case (moduleId, author, mcJson, role, studyProgram, status, _, id) =>
            val protocol =
              Json.fromJson(mcJson)(moduleCompendiumProtocolFormat).get
            val summaryStatus = summaryStatus0(entries.map(_._6)).get
            val canReview = summaryStatus match {
              case WaitingForChanges      => false
              case WaitingForReview(_, _) => status == Pending
            }
            ReviewerApproval(
              id,
              moduleId,
              protocol.metadata.title,
              protocol.metadata.abbrev,
              author,
              role,
              summaryStatus,
              studyProgram,
              canReview
            )
        }
      })
  }

  def hasPendingApproval(reviewId: UUID, user: User): Future[Boolean] =
    approvalRepository.hasPendingApproval(reviewId, user)

  private def summaryStatus0(
      xs: Seq[ModuleReviewStatus]
  ): Option[ModuleReviewSummaryStatus] =
    Option.when(xs.nonEmpty) {
      val (approved, rejected) =
        xs.foldLeft((0, 0)) { case (acc, s) =>
          s match {
            case Approved =>
              acc.focus(_._1).modify(_ + 1)
            case Rejected =>
              acc.focus(_._2).modify(_ + 1)
            case Pending => acc
          }
        }
      if (rejected > 0) WaitingForChanges
      else WaitingForReview(approved, xs.size)
    }
}
