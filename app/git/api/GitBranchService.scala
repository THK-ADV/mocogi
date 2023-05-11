package git.api

import database.repo.UserBranchRepository
import git.GitConfig
import models.{User, UserBranch}
import play.api.libs.ws.{EmptyBody, WSClient}
import play.mvc.Http.Status

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitBranchService @Inject() (
    private val userBranchRepository: UserBranchRepository, // TODO remove (like GitCommitService SRP)
    private val ws: WSClient,
    val gitConfig: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {
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
            branch <- createBranchApiRequest(user)
            res <- userBranchRepository.create(
              UserBranch(user, branch, None, None)
            )
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
            _ <- deleteBranchApiRequest(user)
            d <- userBranchRepository.delete(user) if d > 0
          } yield ()
    } yield res

  private def createBranchApiRequest(user: String): Future[String] = {
    val branchName = this.branchName(user)
    ws
      .url(this.branchUrl())
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        ("branch", branchName),
        ("ref", gitConfig.mainBranch)
      )
      .post(EmptyBody)
      .flatMap { res =>
        if (res.status == Status.CREATED) Future.successful(branchName)
        else Future.failed(parseErrorMessage(res))
      }
  }

  private def deleteBranchApiRequest(user: String): Future[String] = {
    val branchName = this.branchName(user)
    ws
      .url(s"${this.branchUrl()}/$branchName")
      .withHttpHeaders(tokenHeader())
      .delete()
      .flatMap { res =>
        if (res.status == Status.NO_CONTENT) Future.successful(branchName)
        else Future.failed(parseErrorMessage(res))
      }
  }

  private def branchName(user: String) =
    user

  private def branchUrl() =
    s"${repositoryUrl()}/branches"
}
