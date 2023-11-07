package service

import database.POOutput
import database.repo.StudyProgramDirectorsRepository.StudyProgramDirector
import database.repo.{
  ModuleDraftRepository,
  ModuleReviewRepository,
  StudyProgramDirectorsRepository
}
import git.api.GitMergeRequestApiService
import models.ModuleReviewStatus.{Approved, Pending, Rejected}
import models._
import ops.FutureOps.{Ops, abort}
import service.ModuleApprovalService.ModuleReviewSummaryStatus
import service.ModuleApprovalService.ModuleReviewSummaryStatus.{
  WaitingForChanges,
  WaitingForReview
}

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
) {

  private type MergeRequest = (MergeRequestId, MergeRequestStatus)

  /** Either creates a merge request with reviewers based on modified keys which
    * need to be reviewed or a merge request which is accepted and merged
    * instantly. The module draft is updated by the merge request id afterwards.
    * @param moduleId
    *   ID of the module draft
    * @param author
    *   Author of the merge request
    * @return
    */
  def create(moduleId: UUID, author: User): Future[Unit] =
    for {
      draft <- draftRepo.getByModule(moduleId)
      mergeRequest <-
        if (draft.modifiedKeys.isEmpty)
          abort(s"no modifications found for module ${draft.module}")
        else if (draft.keysToBeReviewed.nonEmpty)
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
      reviewer: User,
      approve: Boolean,
      comment: Option[String]
  ): Future[Unit] = {
    val newStatus = if (approve) Approved else Rejected

    def commentBody(summaryStatus: ModuleReviewSummaryStatus) = {
      val statusString = summaryStatus match {
        case WaitingForChanges =>
          summaryStatus.enLabel
        case WaitingForReview(approved, needed) =>
          s"${summaryStatus.enLabel} ($approved/$needed)"
      }

      val body =
        s"""Author: ${reviewer.username}
           |
           |Status: $statusString
           |
           |Action: ${newStatus.id}""".stripMargin
      comment.fold(body)(c => s"$body\nComment: $c")
    }

    for {
      review <- reviewRepo
        .get(id)
        .abortIf(_.isEmpty, "no module review was found")
        .map(_.get)
      draft <- draftRepo
        .getByModuleOpt(review.moduleDraft)
        .abortIf(
          _.forall(_.mergeRequestId.isEmpty),
          "no module draft or merge request id found"
        )
        .map(_.get)
      status <- approvalService
        .summaryStatus(draft.module)
        .abortIf(
          a => a.forall(_ == WaitingForChanges),
          "can't review if the status is waiting for changes"
        )
      mergeRequestId = draft.mergeRequestId.get
      _ <- reviewRepo.update(id, newStatus, comment)
      _ <- api.comment(mergeRequestId, commentBody(status.get))
      _ <-
        if (approve) {
          for {
            reviews <- reviewRepo.getByModule(draft.module)
            _ <-
              if (reviews.forall(_.status == Approved)) {
                for {
                  _ <- api.approve(mergeRequestId)
                  status <- api.accept(mergeRequestId)
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

  private def createApproveReview(
      draft: ModuleDraft,
      author: User
  ): Future[MergeRequest] = {
    val protocol = draft.protocol()
    val roles = requiredRoles(draft.keysToBeReviewed)

    def reviews(directors: Seq[StudyProgramDirector]) =
      roles.toSeq
        .flatMap(role =>
          directors
            .filter(_.role == role)
            .distinctBy(_.studyProgramAbbrev)
            .map(d =>
              ModuleReview(
                UUID.randomUUID,
                draft.module,
                role,
                Pending,
                d.studyProgramAbbrev,
                None
              )
            )
        )

    def reviewer(directors: Seq[StudyProgramDirector]): Iterable[String] =
      directors.groupBy(_.role).flatMap { case (role, dirs) =>
        dirs
          .filter(_.role == role)
          .distinctBy(_.studyProgramAbbrev)
          .map { dir =>
            val possibleDirs = dirs
              .filter(d =>
                d.role == role && d.studyProgramAbbrev == dir.studyProgramAbbrev
              )
              .map(d => d.campusId.getOrElse(s"NOT FOUND ${d.directorId}"))
              .mkString(", ")
            s"${role.id.toUpperCase} ${dir.studyProgramLabel} ($possibleDirs)"
          }
      }

    for {
      directors <- studyProgramDirectors(protocol.metadata.po, roles)
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
      author: User
  ): Future[MergeRequest] =
    for {
      (mergeRequestId, _) <- createMergeRequest(
        draft,
        mrTitle(author, draft.protocol().metadata),
        mrDesc(draft.modifiedKeys, Nil, Nil),
        needsApproval = false
      )
      _ <- api.canBeMerged(mergeRequestId)
      status <- api.accept(mergeRequestId)
    } yield (mergeRequestId, status)

  private def createMergeRequest(
      draft: ModuleDraft,
      title: String,
      description: String,
      needsApproval: Boolean
  ): Future[MergeRequest] =
    draft.mergeRequest match {
      case Some(mergeRequest) =>
        Future.successful(mergeRequest)
      case None =>
        api.create(
          draft.branch,
          Branch(api.config.draftBranch),
          title,
          description,
          needsApproval,
          List(
            if (needsApproval) api.config.reviewApprovedLabel
            else api.config.autoApprovedLabel
          )
        )
    }

  private def mrTitle(author: User, metadata: MetadataProtocol) =
    s"${author.username}: ${metadata.title} (${metadata.abbrev})"

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
