package git.api

import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.*
import models.JsonParseException
import play.api.http.ContentTypes
import play.api.libs.json.*
import play.api.libs.ws.writeableOf_JsValue
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSResponse
import play.mvc.Http.HeaderNames
import play.mvc.Http.Status

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
        "branch"         -> branch.value,
        "commit_message" -> message,
        "author_email"   -> authorEmail,
        "author_name"    -> authorName,
        "actions" -> actions.map { a =>
          a.action match {
            case GitCommitActionType.Create =>
              Json.obj(
                "action"    -> a.action.toString,
                "file_path" -> a.filePath.value,
                "content"   -> a.fileContent
              )
            case GitCommitActionType.Delete =>
              Json.obj(
                "action"    -> a.action.toString,
                "file_path" -> a.filePath.value
              )
            case GitCommitActionType.Update =>
              Json.obj(
                "action"    -> a.action.toString,
                "file_path" -> a.filePath.value,
                "content"   -> a.fileContent
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

  def getCommitDate(sha: String): Future[LocalDateTime] =
    ws.url(s"${this.commitUrl()}/$sha").withHttpHeaders(tokenHeader(), contentTypeJson()).get().flatMap { resp =>
      if resp.status == Status.OK then
        resp.json
          .\("committed_date")
          .validate[LocalDateTime]
          .fold(a => Future.failed(JsonParseException(a)), Future.successful)
      else Future.failed(parseErrorMessage(resp))
    }

  def getCommitDate(path: GitFilePath, branch: Branch): Future[Option[LocalDateTime]] =
    ws.url(this.commitUrl())
      .withQueryStringParameters("path" -> path.value, "ref_name" -> branch.value, "per_page" -> "1")
      .withHttpHeaders(tokenHeader(), contentTypeJson())
      .get()
      .flatMap { resp =>
        if resp.status == Status.OK then
          Future.successful(
            resp.json
              .validate[JsArray]
              .flatMap(_.head.\("committed_date").validate[LocalDateTime])
              .asOpt
          )
        else Future.failed(parseErrorMessage(resp))
      }

  def getCommitDiff(sha: String): Future[List[CommitDiff]] = {
    def parseJson(js: JsValue): List[CommitDiff] =
      js.validate[List[CommitDiff]].fold(_ => List.empty, identity)

    def go(url: String): Future[List[CommitDiff]] =
      ws.url(url)
        .withHttpHeaders(tokenHeader(), contentTypeJson())
        .get()
        .flatMap { resp =>
          if resp.status != Status.OK then Future.failed(parseErrorMessage(resp))
          else {
            val commits = parseJson(resp.json)
            parseNextPaginationUrl(resp) match
              case Some(nextUrl) => go(nextUrl).map(_ ::: commits)
              case None          => Future.successful(commits)
          }
        }

    go(s"${this.commitUrl()}/$sha/diff")
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
