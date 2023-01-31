package git

import database.repo.{UserBranchRepository, UserRepository}
import models.{User, UserBranch}
import play.api.libs.ws.{EmptyBody, WSClient}
import play.mvc.Http.Status

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitBranchService @Inject() (
    private val userBranchRepository: UserBranchRepository, // TODO remove (like GitCommitService SRP)
    private val userRepository: UserRepository, // TODO remove (like GitCommitService SRP)
    private val ws: WSClient,
    val gitConfig: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {
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
            res <- userBranchRepository.create(
              UserBranch(user.id, branch, None)
            )
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

  private def branchName(user: User) =
    s"${user.username}_${user.id}"

  private def branchUrl() =
    s"${baseUrl()}/branches"
}
