package service

import database.POOutput
import database.repo.core.StudyProgramDirectorsRepository
import database.repo.core.StudyProgramDirectorsRepository.StudyProgramDirector
import database.repo.{
  ModuleDraftRepository,
  ModuleReviewRepository
}
import git.api.GitMergeRequestApiService
import models.ModuleReviewStatus.{Approved, Pending, Rejected}
import models._
import models.core.Identity
import ops.FutureOps.Ops
import play.api.Logging

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleReviewService @Inject() (
    private val draftRepo: ModuleDraftRepository, // READ UPDATE ONLY
    private val reviewRepo: ModuleReviewRepository, // CREATE READ UPDATE DELETE
    private val approvalService: ModuleApprovalService, // READ ONLY
    private val directorsRepository: StudyProgramDirectorsRepository,
    private val api: GitMergeRequestApiService,
    private val keysToReview: ModuleKeysToReview,
    private implicit val ctx: ExecutionContext
) extends Logging {

  private type MergeRequest = (MergeRequestId, MergeRequestStatus)

  /** Either creates a merge request with fresh reviewers based on modified keys
    * which need to be reviewed or a merge request which is accepted and merged
    * instantly. The module draft is updated by the merge request id afterwards.
    * @param moduleId
    *   ID of the module draft
    * @param author
    *   Author of the merge request
    * @return
    */
  def create(moduleId: UUID, author: Identity.Person): Future[Unit] =
    for {
      draft <- draftRepo
        .getByModule(moduleId)
        .continueIf(
          _.state().canRequestReview,
          "can't request a review"
        )
      mergeRequest <-
        if (draft.keysToBeReviewed.nonEmpty)
          createApproveReview(draft, author)
        else createAutoAcceptedReview(draft, author)
      _ <- draftRepo.updateMergeRequest(draft.module, Some(mergeRequest))
    } yield ()

  /** Deletes the merge request and all reviews associated with the module id.
    * Removes the merge request id and status from the module draft afterwards.
    * @param moduleId
    *   ID of the module draft which needs to be deleted
    * @return
    */
  def delete(moduleId: UUID): Future[Unit] = {
    def go(id: MergeRequestId) =
      for {
        _ <- api.delete(id)
        _ <- reviewRepo.delete(moduleId)
        _ <- draftRepo.updateMergeRequest(moduleId, None)
      } yield ()
    draftRepo.getByModuleOpt(moduleId).flatMap {
      case Some(draft) if draft.mergeRequestId.isDefined =>
        go(draft.mergeRequestId.get)
      case _ => Future.unit
    }
  }

  /** Updates the review by changing the status to approved or rejected and
    * setting the comment if defined. Updates the merge request associated by
    * the underlying module with status updates. Performs the following actions
    * based on the value of approve:
    *   - If true, checks if all approvals are set. If so, the merge request
    *     will be approved and merged right after. The module draft's status is
    *     updated to merged respectively.
    *   - If false, the merge request will be closed and the module draft's
    *     status is updated respectively.
    *
    * @param id
    *   ID of the review which will be updated
    * @param reviewer
    *   The reviewer
    * @param approve
    *   Whether the review was approved or not
    * @param comment
    *   Optional comment from the reviewer
    * @return
    */
  def update(
      id: UUID,
      reviewer: Identity.Person,
      approve: Boolean,
      comment: Option[String]
  ): Future[Unit] = {
    val newStatus = if (approve) Approved else Rejected

    def commentBody(summaryStatus: ModuleReviewSummaryStatus) = {
      val body =
        s"""Author: ${reviewer.fullName}
           |
           |Status: ${summaryStatus.enLabel}
           |
           |Action: ${newStatus.enLabel}""".stripMargin
      comment.fold(body)(c => s"$body\n\nComment: $c")
    }

    for {
      review <- reviewRepo
        .get(id)
        .abortIf(_.isEmpty, "no module review was found")
        .map(_.get)
      draft <- draftRepo
        .getByModuleOpt(review.moduleDraft)
        .abortIf(
          _.forall(d =>
            d.mergeRequestId.isEmpty || d
              .state() != ModuleDraftState.WaitingForReview
          ),
          "one of the following errors occurred: no module draft found. no merge request id found. status is not waiting for review"
        )
        .map(_.get)
      mergeRequestId = draft.mergeRequestId.get
      _ <- reviewRepo.update(id, newStatus, comment, reviewer.id)
      status <- approvalService.summaryStatus(draft.module)
      _ <- api.comment(mergeRequestId, commentBody(status.get))
      _ <-
        if (approve) {
          for {
            reviews <- reviewRepo.getStatusByModule(draft.module)
            _ <-
              if (reviews.forall(_ == Approved)) {
                for {
                  _ <- api.approve(mergeRequestId)
                  status <- api.merge(mergeRequestId)
                  _ <- draftRepo.updateMergeRequestStatus(draft.module, status)
                } yield ()
              } else {
                Future.unit
              }
          } yield ()
        } else {
          for {
            status <- api.close(mergeRequestId)
            _ <- draftRepo.updateMergeRequestStatus(draft.module, status)
          } yield ()
        }
    } yield ()
  }

  def allByModule(moduleId: UUID): Future[Seq[ModuleReview.Atomic]] =
    reviewRepo.getAtomicByModule(moduleId)

  private def createApproveReview(
      draft: ModuleDraft,
      author: Identity.Person
  ): Future[MergeRequest] = {
    val protocol = draft.protocol()
    val roles = requiredRoles(draft.keysToBeReviewed)

    def reviews(directors: Seq[StudyProgramDirector]): Seq[ModuleReview.DB] =
      roles.toSeq
        .flatMap(role =>
          directors
            .filter(_.role == role)
            .distinctBy(_.studyProgramId)
            .map(d =>
              ModuleReview(
                UUID.randomUUID,
                draft.module,
                role,
                Pending,
                d.studyProgramId,
                None,
                None,
                None
              )
            )
        )

    def reviewer(directors: Seq[StudyProgramDirector]): Iterable[String] =
      directors.groupBy(_.role).flatMap { case (role, dirs) =>
        dirs
          .filter(_.role == role)
          .distinctBy(_.studyProgramId)
          .map { dir =>
            val possibleDirs = dirs
              .filter(d =>
                d.role == role && d.studyProgramId == dir.studyProgramId
              )
              .map(d => s"${d.directorFirstname} ${d.directorLastname}")
              .mkString(", ")
            s"${role.id.toUpperCase} ${dir.studyProgramLabel} ${dir.studyProgramDegreeLabel} ($possibleDirs)"
          }
      }

    for {
      directors <- studyProgramDirectors(protocol.metadata.po, roles)
      _ <- reviewRepo.delete(draft.module)
      _ <- reviewRepo.createMany(reviews(directors))
      mergeRequest <- createMergeRequest(
        draft,
        mrTitle(author, protocol.metadata),
        mrDesc(draft.modifiedKeys, draft.keysToBeReviewed, reviewer(directors)),
        needsApproval = true
      )
    } yield mergeRequest
  }

  private def createAutoAcceptedReview(
      draft: ModuleDraft,
      author: Identity.Person
  ): Future[MergeRequest] =
    for {
      (mergeRequestId, status) <- createMergeRequest(
        draft,
        mrTitle(author, draft.protocol().metadata),
        mrDesc(draft.modifiedKeys, Nil, Nil),
        needsApproval = false
      )
    } yield (mergeRequestId, status)

  private def createMergeRequest(
      draft: ModuleDraft,
      title: String,
      description: String,
      needsApproval: Boolean
  ): Future[MergeRequest] =
    api.create(
      draft.branch,
      api.config.draftBranch,
      title,
      description,
      needsApproval,
      List(
        if (needsApproval) api.config.reviewApprovedLabel
        else api.config.autoApprovedLabel
      )
    )

  private def mrTitle(author: Identity.Person, metadata: MetadataProtocol) =
    s"${author.fullName}: ${metadata.title} (${metadata.abbrev})"

  private def mrDesc(
      modifiedKeys: Iterable[String],
      keysToBeReviewed: Iterable[String],
      reviewer: Iterable[String]
  ) = {
    def go[A](as: Iterable[A]): String =
      as.foldLeft("") { case (acc, a) => s"$acc\n- $a" }

    if (keysToBeReviewed.isEmpty && reviewer.isEmpty)
      s"""modified keys:
         |${go(modifiedKeys)}""".stripMargin
    else s"""modified keys:
            |${go(modifiedKeys)}
            |
            |keys to be reviewed:
            |${go(keysToBeReviewed)}
            |
            |reviewer:
            |${go(reviewer)}""".stripMargin
  }

  private def requiredRoles(
      keysToBeReviewed: Set[String]
  ): Set[UniversityRole] =
    keysToBeReviewed.foldLeft(Set.empty[UniversityRole]) { case (acc, key) =>
      if (keysToReview.isSGLReview(key)) acc + UniversityRole.SGL
      else if (keysToReview.isPAVReview(key)) acc + UniversityRole.PAV
      else acc
    }

  private def studyProgramDirectors(
      po: POOutput,
      roles: Set[UniversityRole]
  ): Future[Seq[StudyProgramDirector]] = {
    def affectedPos(): Set[String] =
      (po.mandatory.isEmpty, po.optional.isEmpty) match {
        case (true, true) =>
          Set.empty
        case (false, false) => // Default + WPF
          po.mandatory.map(_.po).toSet
        case (true, false) => // WPF only
          po.optional.map(_.po).toSet
        case (false, true) => // Default only
          po.mandatory.map(_.po).toSet
      }
    directorsRepository.all(affectedPos(), roles)
  }
}
