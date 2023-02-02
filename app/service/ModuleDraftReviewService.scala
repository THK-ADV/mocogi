package service

import database.repo.UserBranchRepository
import git.{
  GitCommitAction,
  GitCommitActionType,
  GitCommitService,
  GitMergeRequestService
}
import models.ModuleDraftStatus

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ModuleDraftReviewService @Inject() (
    private val moduleDraftService: ModuleDraftService,
    private val moduleCompendiumService: ModuleCompendiumService,
    private val commitService: GitCommitService,
    private val mergeRequestService: GitMergeRequestService,
    private val userBranchRepository: UserBranchRepository,
    private implicit val ctx: ExecutionContext
) {

  type ReviewResult =
    (GitCommitService#CommitID, GitMergeRequestService#MergeRequestID)

  def createReview(
      branch: String,
      username: String
  ): Future[ReviewResult] =
    for {
      hasCommit <- userBranchRepository.hasCommitAndMergeRequest(branch)
      drafts <- continueIf(
        hasCommit.isEmpty,
        moduleDraftService.validDrafts(branch),
        s"user $username has already committed"
      )
      res <- continueIf(
        drafts.nonEmpty,
        for {
          actions <- commitActions(drafts)
          commitId <- commit(branch, username, actions)
          mergeRequestId <- createMergeRequest(
            branch,
            username,
            mergeRequestDescription(actions)
          )
        } yield (commitId, mergeRequestId),
        "no changes to commit"
      )
    } yield res

  private def continueIf[A](
      bool: Boolean,
      future: => Future[A],
      err: => String
  ) =
    if (bool) future else Future.failed(new Throwable(err))

  private def mergeRequestDescription(actions: Seq[GitCommitAction]) =
    actions.foldLeft("") { case (acc, a) =>
      s"$acc\n- ${a.action}s [${a.filename}](${a.filename})"
    }

  private def createMergeRequest(
      branch: String,
      username: String,
      description: String
  ) =
    for {
      mergeRequestId <- mergeRequestService.createMergeRequest(
        branch,
        username,
        description
      )
      _ <- userBranchRepository.updateMergeRequestId(
        branch,
        Some(mergeRequestId)
      )
    } yield mergeRequestId

  private def commitActions(drafts: Seq[ModuleDraftService#ValidDraft]) =
    for {
      paths <- moduleCompendiumService.paths(drafts.map(_._1))
    } yield drafts.map { case (id, status, print) =>
      val gitActionType = status match {
        case ModuleDraftStatus.Added    => GitCommitActionType.Create
        case ModuleDraftStatus.Modified => GitCommitActionType.Update
      }
      val filename = paths.find(_._1 == id) match {
        case Some((_, path)) => path.value
        case None            => s"${id.toString}.md"
      }
      GitCommitAction(gitActionType, filename, print)
    }

  private def commit(
      branch: String,
      username: String,
      actions: Seq[GitCommitAction]
  ) =
    for {
      commitId <- commitService.commit(branch, username, actions)
      _ <- userBranchRepository.updateCommitId(branch, Some(commitId))
    } yield commitId

  def revertReview(branch: String) =
    for {
      maybeCommit <- userBranchRepository.hasCommitAndMergeRequest(branch)
      res <- maybeCommit match {
        case Some((commitId, mergeRequestId)) =>
          for {
            _ <- commitService.revertCommit(branch, commitId)
            _ <- userBranchRepository.updateCommitId(branch, None)
            _ <- mergeRequestService.deleteMergeRequest(mergeRequestId)
            _ <- userBranchRepository.updateMergeRequestId(branch, None)
          } yield ()
        case None =>
          Future.failed(
            new Throwable(
              s"branch $branch has no commit or merge request to revert"
            )
          )
      }
    } yield res
}
