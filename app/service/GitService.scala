package service

import database.repo.{UserBranchRepository, UserRepository}
import git.GitConfig
import models.{User, UserBranch}
import play.api.libs.ws.{EmptyBody, WSClient, WSResponse}
import play.mvc.Http.Status

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitService @Inject() (
    private val userBranchRepository: UserBranchRepository,
    private val userRepository: UserRepository,
    private val ws: WSClient,
    private val gitConfig: GitConfig,
    private implicit val ctx: ExecutionContext
) {
  def branchForUser(username: String): Future[Option[UserBranch]] =
    userBranchRepository.branchForUser(username)

  def createBranch(username: String): Future[UserBranch] =
    for {
      user <- userRepository.byUsername(username)
      exists <- userBranchRepository.exists(user.id)
      res <-
        if (exists)
          Future.failed(
            new Throwable(s"branch for user $username already exists")
          )
        else
          for {
            branch <- createBranchApiRequest(user)
            res <- userBranchRepository.create(models.UserBranch(user.id, branch))
          } yield res
    } yield res

  def deleteBranch(username: String) =
    for {
      user <- userRepository.byUsername(username)
      exists <- userBranchRepository.exists(user.id)
      res <-
        if (!exists)
          Future.failed(
            new Throwable(s"branch for user $username doesn't exists")
          )
        else
          for {
            _ <- deleteBranchApiRequest(user)
            d <- userBranchRepository.delete(user.id) if d > 0
          } yield ()
    } yield res

  private def createBranchApiRequest(user: User): Future[String] = {
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

  private def deleteBranchApiRequest(user: User): Future[String] = {
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

  private def parseErrorMessage(res: WSResponse) =
    res.json
      .\("message")
      .validate[String]
      .fold(
        errs => new Throwable(errs.mkString("\n")),
        msg => new Throwable(msg)
      )

  private def branchName(user: User) =
    s"${user.username}_${user.id}"

  private def branchUrl() =
    s"${gitConfig.baseUrl}/projects/${gitConfig.projectId}/repository/branches"

  private def tokenHeader() =
    ("PRIVATE-TOKEN", gitConfig.accessToken)
}
