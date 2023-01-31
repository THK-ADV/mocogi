package service

import database.repo.UserBranchRepository
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

  def createReview(branch: String, username: String): Future[CommitID] =
    for {
      hasCommit <- userBranchRepository.hasCommit(branch)
      res <-
        if (hasCommit)
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
}
