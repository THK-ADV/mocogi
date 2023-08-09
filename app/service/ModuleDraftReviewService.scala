package service

import database.repo.UserBranchRepository
import git.api.{GitCommitApiService, GitMergeRequestApiService}
import git.{GitCommitAction, GitCommitActionType, GitConfig, GitFilePath}
import models.{ModuleDraftStatus, ValidModuleDraft}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ModuleDraftReviewService @Inject() (
    private val moduleDraftService: ModuleDraftService,
    private val moduleCompendiumService: ModuleCompendiumService,
    private val commitService: GitCommitApiService,
    private val mergeRequestService: GitMergeRequestApiService,
    private val userBranchRepository: UserBranchRepository,
    private implicit val gitConfig: GitConfig,
    private implicit val ctx: ExecutionContext
) {

  type ReviewResult =
    (GitCommitApiService#CommitID, GitMergeRequestApiService#MergeRequestID)

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
      s"$acc\n- ${a.action}s [${a.filePath.value}](${a.filePath.value})"
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

  private def commitActions(drafts: Seq[ValidModuleDraft]) =
    for {
      paths <- moduleCompendiumService.paths(drafts.map(_.module))
    } yield drafts.map { draft =>
      val gitActionType = draft.status match {
        case ModuleDraftStatus.Added    => GitCommitActionType.Create
        case ModuleDraftStatus.Modified => GitCommitActionType.Update
      }
      val filePath = GitFilePath.apply(paths, draft)
      GitCommitAction(gitActionType, filePath, draft.print.value)
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
