package service

import database.repo.{UserBranchRepository, UserRepository}
import git.GitConfig
import models.{User, UserBranch}
import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{EmptyBody, WSClient, WSResponse}
import play.mvc.Http.Status

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

sealed trait GitCommitActionType {
  override def toString = this match {
    case GitCommitActionType.Create => "create"
    case GitCommitActionType.Delete => "delete"
    case GitCommitActionType.Update => "update"
  }
}

object GitCommitActionType {
  case object Create extends GitCommitActionType
  case object Delete extends GitCommitActionType
  case object Update extends GitCommitActionType
}

case class GitCommitAction(
    action: GitCommitActionType,
    filename: String,
    fileContent: String
)

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

  def commit(
      branchName: String,
      username: String,
      actions: Seq[GitCommitAction]
  ): Future[String] = {
    def commitBody(): JsValue =
      Json.obj(
        "branch" -> branchName,
        "commit_message" -> "changes",
        "author_email" -> s"$username@th-koeln.de",
        "author_name" -> username,
        "actions" -> actions.map { a =>
          a.action match {
            case GitCommitActionType.Create =>
              Json.obj(
                "action" -> a.action.toString,
                "file_path" -> s"${gitConfig.modulesRootFolder}/${a.filename}",
                "content" -> a.fileContent
              )
            case GitCommitActionType.Delete =>
              Json.obj(
                "action" -> a.action.toString,
                "file_path" -> a.filename
              )
            case GitCommitActionType.Update =>
              Json.obj(
                "action" -> a.action.toString,
                "file_path" -> a.filename,
                "content" -> a.fileContent
              )
          }
        }
      )
    def parseResult(js: JsValue) =
      js.\("id").validate[String].get

    ws
      .url(this.commitUrl())
      .withHttpHeaders(tokenHeader(), contentTypeJson())
      .post(commitBody())
      .flatMap { res =>
        if (res.status == Status.CREATED)
          Future.successful(parseResult(res.json))
        else Future.failed(parseErrorMessage(res))
      }
  }

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

  private def baseUrl() =
    s"${gitConfig.baseUrl}/projects/${gitConfig.projectId}/repository"

  private def branchUrl() =
    s"${baseUrl()}/branches"

  private def commitUrl() =
    s"${baseUrl()}/commits"

  private def tokenHeader() =
    ("PRIVATE-TOKEN", gitConfig.accessToken)

  private def contentTypeJson() =
    (HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
}
