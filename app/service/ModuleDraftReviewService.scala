package service

import database.repo.UserBranchRepository
import git.{GitCommitAction, GitCommitActionType, GitService}
import models.ModuleDraftStatus

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ModuleDraftReviewService @Inject() (
    private val moduleDraftService: ModuleDraftService,
    private val moduleCompendiumService: ModuleCompendiumService,
    private val gitService: GitService,
    private val userBranchRepository: UserBranchRepository,
    private implicit val ctx: ExecutionContext
) {
  type CommitID = String

  def commitDrafts(branch: String, username: String): Future[CommitID] =
    for {
      hasCommit <- userBranchRepository.hasCommit(branch)
      res <-
        if (hasCommit.isDefined)
          Future.failed(new Throwable(s"user $username has already committed"))
        else
          for {
            drafts <- moduleDraftService.validDrafts(branch)
            paths <- moduleCompendiumService.paths(drafts.map(_._1))
            actions = drafts.map { case (id, status, print) =>
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
            commitId <- gitService.commit(branch, username, actions)
            _ <- userBranchRepository.updateCommitId(branch, Some(commitId))
          } yield commitId
    } yield res

  def revertCommit(branch: String) =
    for {
      maybeCommit <- userBranchRepository.hasCommit(branch)
      res <- maybeCommit match {
        case Some(commit) =>
          for {
            _ <- gitService.revertCommit(branch, commit)
            _ <- userBranchRepository.updateCommitId(branch, None)
          } yield ()
        case None =>
          Future.failed(
            new Throwable(s"branch $branch has no commit to revert")
          )
      }
    } yield res
}
