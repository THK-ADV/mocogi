package git.api

import database.repo.UserBranchRepository
import models.UserBranch

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitBranchService @Inject() (
    private val userBranchRepository: UserBranchRepository,
    private val apiService: GitBranchApiService,
    implicit val ctx: ExecutionContext
) {
  def branchForUser(username: String): Future[Option[UserBranch]] =
    userBranchRepository.branchForUser(username)

  def createBranch(user: String): Future[UserBranch] =
    for {
      exists <- userBranchRepository.existsByUser(user)
      res <-
        if (exists)
          Future.failed(
            new Throwable(s"branch for user $user already exists")
          )
        else
          for {
            branch <- apiService.createBranch(user)
            res <- userBranchRepository.create(branch)
          } yield res
    } yield res

  def deleteBranch(user: String) =
    for {
      exists <- userBranchRepository.existsByUser(user)
      res <-
        if (!exists)
          Future.failed(
            new Throwable(s"branch for user $user doesn't exists")
          )
        else
          for {
            _ <- apiService.deleteBranch(user)
            d <- userBranchRepository.delete(user) if d > 0
          } yield ()
    } yield res
}
