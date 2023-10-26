package service

import database.repo.ModuleReviewerRepository
import git.api.GitMergeRequestApiService
import models._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleDraftReviewService @Inject() (
    private val moduleDraftService: ModuleDraftService,
    private val moduleReviewService: ModuleReviewService,
    private val moduleReviewerRepo: ModuleReviewerRepository,
    private val api: GitMergeRequestApiService,
    private val keysToReview: ModuleKeysToReview,
    private implicit val ctx: ExecutionContext
) {

  def create(moduleId: UUID, author: User) =
    for {
      draft <- moduleDraftService.getByModule(moduleId)
      _ <-
        if (draft.modifiedKeys.isEmpty)
          Future.failed(
            new Throwable(s"no modifications found for module ${draft.module}")
          )
        else {
          if (draft.keysToBeReviewed.nonEmpty)
            createApproveReview(draft, author)
          else createAutoAcceptReview(draft, author)
        }
    } yield ()

  def close(moduleId: UUID) =
    for {
      draft <- moduleDraftService.getByModule(moduleId)
      if draft.mergeRequest.isDefined
      res <- api.delete(draft.mergeRequest.get)
      _ <- moduleDraftService.updateMergeRequestId(draft.module, None)
      _ <- moduleReviewService.delete(moduleId)
    } yield res

  def delete(moduleId: UUID, mergeRequestId: MergeRequestId) =
    for {
      _ <- api.delete(mergeRequestId)
      _ <- moduleReviewService.delete(moduleId)
    } yield ()

  private def createApproveReview(draft: ModuleDraft, author: User) = {
    def description(reviewer: Seq[ModuleReviewer]) =
      s"""modified keys:
         |${markdownList(draft.modifiedKeys)(identity)}
         |
         |keys to be reviewed:
         |${markdownList(draft.keysToBeReviewed)(identity)}
         |
         |reviewer:
         |${markdownList(reviewer)(r =>
          s"@${r.user.username} ${r.role.label} ${r.studyProgram}"
        )}""".stripMargin
    val protocol = draft.protocol()
    for {
      reviewer <- moduleReviewerRepo.getAll(
        requiredRoles(draft.keysToBeReviewed),
        affectedPOs(protocol.metadata)
      )
      _ <- createMergeRequest(
        draft,
        title(author, protocol),
        description(reviewer),
        needsApproval = true
      )
      _ <- createReviews(draft.module, reviewer)
    } yield ()
  }

  private def createAutoAcceptReview(draft: ModuleDraft, author: User) =
    for {
      mrId <- createMergeRequest(
        draft,
        title(author, draft.protocol()),
        s"""modified keys:
           |${markdownList(draft.modifiedKeys)(identity)}""".stripMargin,
        needsApproval = false
      )
      _ <- api.canBeMerged(mrId)
      _ <- api.accept(mrId)
    } yield ()

  private def createMergeRequest(
      draft: ModuleDraft,
      title: String,
      description: String,
      needsApproval: Boolean
  ) =
    draft.mergeRequest match {
      case Some(mrId) =>
        Future.successful(mrId)
      case None =>
        for {
          mrId <- api.create(
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
          _ <- moduleDraftService.updateMergeRequestId(draft.module, Some(mrId))
        } yield mrId
    }

  private def createReviews(module: UUID, reviewer: Seq[ModuleReviewer]) =
    moduleReviewService.create(
      ModuleReview(
        module,
        ModuleReviewStatus.WaitingForApproval,
        reviewer.map(r => ModuleReviewRequest(r.id, approved = false))
      )
    )

  private def title(author: User, protocol: ModuleCompendiumProtocol) =
    s"@${author.username}: ${protocol.metadata.title} (${protocol.metadata.abbrev})"

  private def requiredRoles(
      keysToBeReviewed: Set[String]
  ): Seq[UniversityRole] = {
    val list = ListBuffer[UniversityRole]()
    if (keysToReview.isSGLReview(keysToBeReviewed))
      list += UniversityRole.SGL
    if (keysToReview.isPAVReview(keysToBeReviewed))
      list += UniversityRole.PAV
    list.result()
  }

  private def markdownList[A](as: Iterable[A])(f: A => String): String =
    as.foldLeft("") { case (acc, a) =>
      s"$acc\n- ${f(a)}"
    }

  private def affectedPOs(metadata: MetadataProtocol): Set[String] =
    metadata.po.mandatory
      .map(_.po)
      .appendedAll(metadata.po.optional.map(_.po))
      .toSet
}
