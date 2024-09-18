package git.api

import git.{Branch, CommitId, GitCommitAction, GitCommitActionType, GitConfig}
import play.api.http.ContentTypes
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.mvc.Http.{HeaderNames, Status}
import play.api.libs.ws.writeableOf_JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitCommitApiService @Inject() (
    private val ws: WSClient,
    val config: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {

  def commit(
      branch: Branch,
      authorEmail: String,
      authorName: String,
      message: String,
      actions: Seq[GitCommitAction]
  ): Future[CommitId] = {
    def body(): JsValue =
      Json.obj(
        "branch" -> branch.value,
        "commit_message" -> message,
        "author_email" -> authorEmail,
        "author_name" -> authorName,
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
    ws
      .url(this.commitUrl())
      .withHttpHeaders(tokenHeader(), contentTypeJson())
      .post(body())
      .flatMap(parseCommitResult)
  }

  private def parseCommitResult(res: WSResponse) =
    if (res.status == Status.CREATED)
      Future.successful(
        res.json.\("id").validate[String].map(CommitId.apply).get
      )
    else Future.failed(parseErrorMessage(res))

  private def commitUrl() =
    s"${repositoryUrl()}/commits"

  private def contentTypeJson() =
    (HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
}
