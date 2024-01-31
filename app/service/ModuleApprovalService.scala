package service

import com.google.inject.{Inject, Singleton}
import database.repo.ModuleApprovalRepository
import models.ModuleReviewStatus.{Approved, Pending, Rejected}
import models.ModuleReviewSummaryStatus.{WaitingForChanges, WaitingForReview}
import models.core.{IDLabel, Identity}
import models.{ModuleReviewStatus, ModuleReviewSummaryStatus, ReviewerApproval}
import monocle.Monocle.toAppliedFocusOps

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleApprovalService @Inject() (
    private val approvalRepository: ModuleApprovalRepository,
    private implicit val ctx: ExecutionContext
) {

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
    * given person (reviewer) can perform a review. A review status is defined
    * as waiting for review (with a progress indicator) or waiting for changes.
    *
    * @param person
    *   The person which requested the reviews
    * @return
    */
  def reviewerApprovals(
      person: Identity.Person
  ): Future[Iterable[ReviewerApproval]] = {
    approvalRepository
      .allByModulesWhereUserExists(person.id)
      .map(_.groupBy(_._1).flatMap { case (_, entries) =>
        entries.filter(_._8.isDefined).map {
          case (
                moduleId,
                moduleTitle,
                moduleAbbrev,
                author,
                role,
                _,
                status,
                sp,
                id
              ) =>
            val studyProgram = (sp.get._1, sp.get._2, sp.get._3)
            val degree = sp.get._4
            val summaryStatus = summaryStatus0(entries.map(_._7)).get
            val canReview = summaryStatus match {
              case WaitingForChanges      => false
              case WaitingForReview(_, _) => status == Pending
            }
            ReviewerApproval(
              id,
              moduleId,
              moduleTitle,
              moduleAbbrev,
              Identity.toPerson(author),
              role,
              summaryStatus,
              IDLabel(studyProgram),
              degree,
              canReview
            )
        }
      })
  }

  /** Returns whether the given person has a pending approval for the review
    * @param reviewId
    *   ID of the review to check against
    * @param person
    *   Person to check against
    * @return
    */
  def hasPendingApproval(
      reviewId: UUID,
      person: Identity.Person
  ): Future[Boolean] =
    approvalRepository.hasPendingApproval(reviewId, person.id)

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
