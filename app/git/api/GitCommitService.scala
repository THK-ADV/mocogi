package git.api

import git.{GitCommitAction, GitCommitActionType, GitConfig}
import play.api.http.ContentTypes
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.mvc.Http.{HeaderNames, Status}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitCommitService @Inject() (
    private val ws: WSClient,
    val gitConfig: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {
  type CommitID = String

  def commit(
      branchName: String,
      username: String,
      actions: Seq[GitCommitAction]
  ): Future[CommitID] =
    ws
      .url(this.commitUrl())
      .withHttpHeaders(tokenHeader(), contentTypeJson())
      .post(commitBody(branchName, username, actions))
      .flatMap(parseCommitResult)

  def revertCommit(branchName: String, commitId: CommitID) =
    ws
      .url(s"${this.commitUrl()}/${commitId}/revert")
      .withHttpHeaders(tokenHeader(), contentTypeForm())
      .post(s"branch=$branchName")
      .flatMap(parseCommitResult)

  private def commitBody(
      branchName: String,
      username: String,
      actions: Seq[GitCommitAction]
  ): JsValue =
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
              "file_path" -> a.filePath.value,
              "content" -> a.fileContent
            )
          case GitCommitActionType.Delete =>
            Json.obj(
              "action" -> a.action.toString,
              "file_path" -> a.filePath.value
            )
          case GitCommitActionType.Update =>
            Json.obj(
              "action" -> a.action.toString,
              "file_path" -> a.filePath.value,
              "content" -> a.fileContent
            )
        }
      }
    )

  private def parseCommitResult(res: WSResponse) =
    if (res.status == Status.CREATED)
      Future.successful(res.json.\("id").validate[CommitID].get)
    else Future.failed(parseErrorMessage(res))

  private def commitUrl() =
    s"${repositoryUrl()}/commits"

  private def contentTypeJson() =
    (HeaderNames.CONTENT_TYPE, ContentTypes.JSON)

  private def contentTypeForm() =
    (HeaderNames.CONTENT_TYPE, ContentTypes.FORM)
}
