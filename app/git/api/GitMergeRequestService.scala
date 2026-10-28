package git.api

import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.*
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.ws.writeableOf_WsBody
import play.api.libs.ws.EmptyBody
import play.api.libs.ws.WSClient
import play.mvc.Http.Status

@Singleton
final class GitMergeRequestService @Inject() (
    private val ws: WSClient,
    protected val apiConfig: GitApiConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {

  /** Returns the five most recently created matching MR titles for the source branch, regardless of state. */
  def latestTitles(sourceBranch: Branch, search: String): Future[List[String]] =
    ws.url(mergeRequestUrl)
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        "source_branch" -> sourceBranch.value,
        "state"         -> "all",
        "scope"         -> "all",
        "search"        -> search,
        "in"            -> "title",
        "order_by"      -> "created_at",
        "sort"          -> "desc",
        "per_page"      -> "5"
      )
      .get()
      .flatMap { res =>
        if res.status != Status.OK then Future.failed(parseErrorMessage(res))
        else Future.successful(res.json.as[List[JsObject]].map(js => (js \ "title").as[String]))
      }

  def create(
      sourceBranch: Branch,
      targetBranch: Branch,
      title: String,
      description: String,
      needsApproval: Boolean,
      labels: List[String]
  ): Future[(MergeRequestId, MergeRequestStatus)] =
    ws
      .url(mergeRequestUrl)
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        "source_branch"          -> sourceBranch.value,
        "target_branch"          -> targetBranch.value,
        "title"                  -> title,
        "description"            -> description,
        "remove_source_branch"   -> true.toString,
        "squash_on_merge"        -> true.toString,
        "squash"                 -> true.toString,
        "approvals_before_merge" -> (if (needsApproval) 1 else 0).toString,
        "labels"                 -> labels.mkString(",")
      )
      .post(EmptyBody)
      .flatMap { res =>
        if (res.status == Status.CREATED)
          Future.successful(
            (
              res.json.\("iid").validate[Int].map(MergeRequestId.apply).get,
              MergeRequestStatus.Open
            )
          )
        else Future.failed(parseErrorMessage(res))
      }

  def delete(id: MergeRequestId): Future[Unit] =
    ws
      .url(closeUrl(id))
      .withHttpHeaders(tokenHeader())
      .delete()
      .flatMap { res =>
        if (res.status == Status.NO_CONTENT) Future.unit
        else Future.failed(parseErrorMessage(res))
      }

  def close(id: MergeRequestId): Future[MergeRequestStatus] =
    ws
      .url(closeUrl(id))
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        "state_event" -> "close"
      )
      .put(EmptyBody)
      .flatMap { res =>
        if (res.status == Status.OK)
          Future.successful(MergeRequestStatus.Closed)
        else Future.failed(parseErrorMessage(res))
      }

  def approve(id: MergeRequestId): Future[MergeRequestStatus] =
    ws
      .url(s"$mergeRequestUrl/${id.value}/approve")
      .withHttpHeaders(tokenHeader())
      .post(EmptyBody)
      .flatMap { res =>
        if (res.status == Status.CREATED) Future.successful(MergeRequestStatus.Approved)
        else Future.failed(parseErrorMessage(res))
      }

  /** With `sha`, GitLab only merges if the source branch still points to the checked commit. */
  def merge(id: MergeRequestId, sha: Option[CommitId] = None): Future[MergeRequestStatus] =
    ws.url(s"$mergeRequestUrl/${id.value}/merge")
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        (Seq("squash" -> "true", "should_remove_source_branch" -> "true") ++ sha.map("sha" -> _.value))*
      )
      .put(EmptyBody)
      .flatMap { res =>
        if (res.status == Status.OK)
          Future.successful(MergeRequestStatus.Merged)
        else Future.failed(parseErrorMessage(res))
      }

  def comment(id: MergeRequestId, body: String): Future[Unit] =
    ws.url(s"$mergeRequestUrl/${id.value}/notes")
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters("body" -> body)
      .post(EmptyBody)
      .flatMap { res =>
        if (res.status == Status.CREATED) Future.unit
        else Future.failed(parseErrorMessage(res))
      }

  def hasOpenedMergeRequests(targetBranch: Branch) =
    ws
      .url(this.mergeRequestUrl)
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        "target_branch" -> targetBranch.value,
        "state"         -> "opened"
      )
      .get()
      .flatMap { res =>
        if (res.status == Status.OK)
          res.json match {
            case JsArray(xs) => Future.successful(xs.nonEmpty)
            case other       => Future.failed(new Exception(s"expected result to be an json array, but was $other"))
          }
        else Future.failed(parseErrorMessage(res))
      }

  def get(mergeRequestId: MergeRequestId) =
    ws
      .url(s"${this.mergeRequestUrl}/${mergeRequestId.value}")
      .withHttpHeaders(tokenHeader())
      .get()
      .flatMap { res =>
        if res.status == Status.OK then Future.successful((res.status, res.json))
        else Future.failed(parseErrorMessage(res))
      }

  def getLatestMergeDate(source: Branch, target: Branch): Future[Option[LocalDate]] = {
    def parse(js: JsValue) =
      for
        array <- js.validate[JsArray]
        date  <- array.head.\("merged_at").validate[LocalDateTime].map(_.toLocalDate)
      yield date

    ws
      .url(this.mergeRequestUrl)
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        "source_branch" -> source.value,
        "target_branch" -> target.value,
        "state"         -> "merged",
        "order_by"      -> "updated_at",
        "sort"          -> "desc"
      )
      .get()
      .flatMap { resp =>
        if resp.status == Status.OK then Future.successful(parse(resp.json).asOpt)
        else Future.failed(parseErrorMessage(resp))
      }
  }

  /**
   * Returns the changed files of the MR. Fails on truncated or invalid diffs, as checks would miss files.
   * With `sha`, also requires the diff version to match the commit used for downloads and merging.
   */
  def getChanges(mergeRequestId: MergeRequestId, sha: Option[CommitId] = None) =
    ws
      .url(s"${this.mergeRequestUrl}/${mergeRequestId.value}/changes")
      .withHttpHeaders(tokenHeader())
      .get()
      .flatMap { res =>
        if res.status != Status.OK then Future.failed(parseErrorMessage(res))
        else if (res.json \ "overflow").asOpt[Boolean].contains(true) then
          Future.failed(IllegalStateException(s"Changes of MR ${mergeRequestId.value} exceed GitLab's diff limits"))
        else {
          val json = res.json
          sha.foreach { expected =>
            val actual = (json \ "diff_refs" \ "head_sha").as[String]
            require(
              actual == expected.value,
              s"Changes of MR ${mergeRequestId.value} belong to commit $actual, expected ${expected.value}"
            )
          }
          Future.successful((json \ "changes").as[List[Diff]])
        }
      }

  private def mergeRequestUrl = s"${projectsUrl()}/merge_requests"

  private def closeUrl(id: MergeRequestId) = s"$mergeRequestUrl/${id.value}"
}
