package service

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.StudyProgramDirectorsRepository
import database.repo.core.StudyProgramDirectorsRepository.StudyProgramDirector
import database.repo.ModuleDraftRepository
import database.repo.ModuleReviewRepository
import git.api.GitMergeRequestApiService
import git.MergeRequestId
import git.MergeRequestStatus
import models.*
import models.core.IDLabel
import models.core.Identity
import models.ModuleReviewStatus.Approved
import models.ModuleReviewStatus.Pending
import models.ModuleReviewStatus.Rejected
import models.ModuleReviewSummaryStatus.WaitingForChanges
import models.ModuleReviewSummaryStatus.WaitingForReview
import ops.FutureOps.Ops
import permission.Permissions
import play.api.Logging

@Singleton
final class ModuleReviewService @Inject() (
    private val draftRepo: ModuleDraftRepository,   // READ UPDATE ONLY
    private val reviewRepo: ModuleReviewRepository, // CREATE READ UPDATE DELETE
    private val directorsRepository: StudyProgramDirectorsRepository,
    private val api: GitMergeRequestApiService,
    private val keysToReview: ModuleKeysToReview,
    private implicit val ctx: ExecutionContext
) extends Logging {

  private type MergeRequest = (MergeRequestId, MergeRequestStatus)

  /**
   * Returns whether the user has the passed reviews open
   */
  def hasPendingReview(reviewIds: List[UUID], person: String): Future[Boolean] =
    reviewRepo.hasPendingReview(reviewIds, person)

  /**
   * Returns true if the person can approve the module.
   * This depends on whether the user is a PAV or has admin permissions
   */
  def canApproveModule(moduleId: UUID, person: String, permissions: Permissions): Future[Boolean] =
    if permissions.isAdmin then Future.successful(true) else reviewRepo.canApproveModule(moduleId, person)

  /**
   * The creation process is two-stage:
   * - 1. If the draft contains changes which need to be reviewed, a merge request is created along with reviews
   * - 2. Otherwise, an instantly accepted and merged request is created.
   * The module draft is updated with the merge request ID afterward.
   *
   * @param moduleId
   *   ID of the module draft
   * @param author
   *   Author of the merge request
   * @return
   */
  def create(moduleId: UUID, author: Identity.Person): Future[Unit] = {
    val draft = draftRepo.getByModule(moduleId).continueIf(_.state().canRequestReview, "can't request a review")
    for {
      draft <- draft
      mergeRequest <-
        if draft.keysToBeReviewed.isEmpty then createAutoAcceptedReview(draft, author)
        else createApproveReview(draft, author)
      _ <- draftRepo.updateMergeRequest(draft.module, Some(mergeRequest))
    } yield {
      val approvalLabel = if draft.keysToBeReviewed.isEmpty then "is auto approved" else "needs review"
      logger.info(
        s"Successfully created merge request by ${author.id} and id ${mergeRequest._1.value} which $approvalLabel"
      )
    }
  }

  /**
   * The merge request is instantly accepted and merged.
   * Ensure that permissions are checked before the function is called.
   *
   * @param moduleId
   * ID of the module draft
   * @param author
   * Author of the merge request
   * @return
   */
  def fastForward(moduleId: UUID, author: Identity.Person): Future[Unit] = {
    val draft = draftRepo.getByModule(moduleId).continueIf(_.state().canRequestReview, "can't request a review")
    for {
      draft        <- draft
      mergeRequest <- createFastForwardReview(draft, author)
      _            <- draftRepo.updateMergeRequest(draft.module, Some(mergeRequest))
    } yield logger.info(
      s"Successfully created merge request by ${author.id} and id ${mergeRequest._1.value} which was instantly approved"
    )
  }

  /**
   * Deletes the merge request and all reviews associated with the module id.
   * Removes the merge request id and status from the module draft afterwards.
   * @param moduleId
   *   ID of the module draft which needs to be deleted
   * @return
   */
  def delete(moduleId: UUID): Future[Unit] = {
    def go(id: Option[MergeRequestId]) =
      for {
        _ <- id.fold(Future.unit)(api.delete)
        _ <- reviewRepo.delete(moduleId)
        _ <- draftRepo.updateMergeRequest(moduleId, None)
      } yield logger.info(s"Successfully deleted reviews for module $moduleId")
    draftRepo.getByModuleOpt(moduleId).flatMap {
      case Some(draft) => go(draft.mergeRequestId)
      case _           => Future.unit
    }
  }

  /**
   * Updates the review by changing the status to approve or rejected and
   * sets the comment if defined. Updates the merge request associated with
   * the underlying module with status updates. Performs the following actions
   * based on the value of approval:
   *   - If true, checks if all approvals are set. If so, the merge request
   *     will be approved and merged right after. The module draft's status is
   *     updated to "merged" respectively.
   *   - If false, the merge request will be closed and the module draft's
   *     status is updated respectively.
   *
   * @param ids
   *   IDs of the reviews which will be updated
   * @param reviewer
   *   The reviewer
   * @param approve
   *   Whether the review was approved or not
   * @param comment
   *   Optional comment from the reviewer
   * @return
   */
  def update(ids: List[UUID], reviewer: Identity.Person, approve: Boolean, comment: Option[String]): Future[Unit] = {
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
      moduleId <- reviewRepo.moduleId(ids)
      draft <- draftRepo
        .getByModuleOpt(moduleId)
        .abortIf(
          _.forall(d =>
            d.mergeRequestId.isEmpty || d
              .state() != ModuleDraftState.WaitingForReview
          ),
          "one of the following errors occurred: no module draft found. no merge request id found. status is not waiting for review"
        )
        .map(_.get)
      mergeRequestId = draft.mergeRequestId.get
      _      <- reviewRepo.update(ids, newStatus, comment, reviewer.id)
      status <- moduleReviewSummaryStatus(draft.module)
      _      <- api.comment(mergeRequestId, commentBody(status.get))
      _ <-
        if (approve) {
          for {
            reviews <- reviewRepo.getStatusByModule(draft.module)
            _ <-
              if (reviews.forall(_ == Approved)) {
                for {
                  _      <- api.approve(mergeRequestId)
                  status <- api.merge(mergeRequestId)
                  _      <- draftRepo.updateMergeRequestStatus(draft.module, status)
                } yield ()
              } else {
                Future.unit
              }
          } yield ()
        } else {
          for {
            status <- api.close(mergeRequestId)
            _      <- draftRepo.updateMergeRequestStatus(draft.module, status)
          } yield ()
        }
    } yield ()
  }

  def allByModule(moduleId: UUID): Future[Seq[ModuleReview.Atomic]] =
    reviewRepo.getAtomicByModule(moduleId)

  /**
   * Returns all reviews with a corresponding review status and whether the
   * given person (reviewer) can perform a review. A review status is defined
   * as waiting for review (with a progress indicator) or waiting for changes.
   *
   * If the user has admin permissions, all reviews are returned.
   *
   * @param person
   * The person who requested the reviews
   * @return
   */
  def moduleReviews(person: Identity.Person, permissions: Permissions): Future[Iterable[ReviewerApproval]] = {
    val reviews = if permissions.isAdmin then reviewRepo.getAllReviews() else reviewRepo.getAllReviewsForUser(person.id)
    reviews.map(_.groupBy(_._1).flatMap {
      case (_, entries) =>
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
            val studyProgram  = (sp.get._1, sp.get._2, sp.get._3)
            val degree        = sp.get._4
            val summaryStatus = ModuleReviewSummaryStatus(entries.map(_._7)).get
            val canReview = summaryStatus match {
              case WaitingForChanges      => false
              case WaitingForReview(_, _) => status == Pending
            }
            ReviewerApproval(
              id,
              moduleId,
              moduleTitle,
              moduleAbbrev,
              Identity.toPersonUnsafe(author),
              role,
              summaryStatus,
              IDLabel(studyProgram),
              degree,
              canReview
            )
        }
    })
  }

  private def moduleReviewSummaryStatus(moduleId: UUID): Future[Option[ModuleReviewSummaryStatus]] =
    reviewRepo.getStatusByModule(moduleId).map(ModuleReviewSummaryStatus.apply)

  private def createApproveReview(draft: ModuleDraft, author: Identity.Person): Future[MergeRequest] = {
    val protocol = draft.protocol()
    val roles    = requiredRoles(draft.keysToBeReviewed)

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
      directors.groupBy(_.role).flatMap {
        case (role, dirs) =>
          dirs
            .filter(_.role == role)
            .distinctBy(_.studyProgramId)
            .sortBy(a => (a.role.id, a.studyProgramLabel, a.studyProgramDegreeLabel))
            .map { dir =>
              val possibleDirs = dirs
                .filter(d => d.role == role && d.studyProgramId == dir.studyProgramId)
                .sortBy(_.directorLastname)
                .map(d => s"${d.directorLastname} ${d.directorFirstname.headOption.fold("")(c => s"$c.")}".trim)
                .mkString(", ")
              s"${role.id.toUpperCase} ${dir.studyProgramLabel} ${dir.studyProgramDegreeLabel} ($possibleDirs)"
            }
      }

    for {
      directors <- studyProgramDirectors(protocol.metadata.po, roles)
      _         <- reviewRepo.delete(draft.module)
      _         <- reviewRepo.createMany(reviews(directors))
      mergeRequest <- createMergeRequest(
        draft,
        mrTitle(author, protocol.metadata),
        mrDesc(draft.modifiedKeys, draft.keysToBeReviewed, reviewer(directors)),
        needsApproval = true,
        List(api.config.reviewRequiredLabel)
      )
    } yield mergeRequest
  }

  private def createAutoAcceptedReview(draft: ModuleDraft, author: Identity.Person): Future[MergeRequest] =
    for {
      (mergeRequestId, status) <- createMergeRequest(
        draft,
        mrTitle(author, draft.protocol().metadata),
        mrDescAutoAccepted(draft.modifiedKeys),
        needsApproval = false,
        List(api.config.autoApprovedLabel)
      )
    } yield (mergeRequestId, status)

  private def createFastForwardReview(draft: ModuleDraft, author: Identity.Person): Future[MergeRequest] =
    for {
      (mergeRequestId, status) <- createMergeRequest(
        draft,
        mrTitle(author, draft.protocol().metadata),
        mrDescAutoAccepted(draft.modifiedKeys),
        needsApproval = false,
        List(api.config.autoApprovedLabel, api.config.fastForwardLabel)
      )
    } yield (mergeRequestId, status)

  private def createMergeRequest(
      draft: ModuleDraft,
      title: String,
      description: String,
      needsApproval: Boolean,
      labels: List[String]
  ): Future[MergeRequest] =
    api.create(
      draft.branch,
      api.config.draftBranch,
      title,
      description,
      needsApproval,
      labels
    )

  private def mrTitle(author: Identity.Person, metadata: MetadataProtocol) =
    s"${author.fullName}: ${metadata.title} (${metadata.abbrev})"

  private def mrDescAutoAccepted(modifiedKeys: Set[String]) =
    mrDesc(modifiedKeys, Set.empty, Nil)

  private def mrDesc(
      modifiedKeys: Set[String],
      keysToBeReviewed: Set[String],
      reviewer: Iterable[String]
  ) = {
    def go(as: List[String]): String =
      as.sorted.foldLeft("") { case (acc, a) => s"$acc\n- $a" }

    if (keysToBeReviewed.isEmpty && reviewer.isEmpty)
      s"""modified keys:
         |${go(modifiedKeys.toList)}""".stripMargin
    else s"""modified keys:
            |${go(modifiedKeys.toList)}
            |
            |keys to be reviewed:
            |${go(keysToBeReviewed.toList)}
            |
            |reviewer:
            |${go(reviewer.toList)}""".stripMargin
  }

  private def requiredRoles(keysToBeReviewed: Set[String]): Set[UniversityRole] =
    keysToBeReviewed.foldLeft(Set.empty[UniversityRole]) {
      case (acc, key) => if (keysToReview.isPAVReview(key)) acc + UniversityRole.PAV else acc
    }

  private def studyProgramDirectors(
      po: ModulePOProtocol,
      roles: Set[UniversityRole]
  ): Future[Seq[StudyProgramDirector]] = {
    def affectedPos(): Set[String] =
      (po.mandatory.isEmpty, po.optional.isEmpty) match {
        case (true, true) =>
          Set.empty
        case (false, false) => // Default + Elective
          po.mandatory.map(_.po).toSet
        case (true, false) => // Elective only
          po.optional.map(_.po).toSet
        case (false, true) => // Default only
          po.mandatory.map(_.po).toSet
      }
    directorsRepository.all(affectedPos(), roles)
  }
}
