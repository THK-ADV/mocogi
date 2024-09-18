package git.api

import git.{Branch, GitConfig, MergeRequestId, MergeRequestStatus}
import play.api.libs.json.JsArray
import play.api.libs.ws.{EmptyBody, WSClient}
import play.mvc.Http.Status
import play.api.libs.ws.writeableOf_WsBody
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitMergeRequestApiService @Inject() (
    private val ws: WSClient,
    val config: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {

  def create(
      sourceBranch: Branch,
      targetBranch: Branch,
      title: String,
      description: String,
      needsApproval: Boolean,
      label: String
  ): Future[(MergeRequestId, MergeRequestStatus)] =
    ws
      .url(mergeRequestUrl)
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        "source_branch" -> sourceBranch.value,
        "target_branch" -> targetBranch.value,
        "title" -> title,
        "description" -> description,
        "remove_source_branch" -> true.toString,
        "squash_on_merge" -> true.toString,
        "squash" -> true.toString,
        "approvals_before_merge" -> (if (needsApproval) 1 else 0).toString,
        "labels" -> label
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

  def approve(id: MergeRequestId): Future[Unit] =
    ws
      .url(s"$mergeRequestUrl/${id.value}/approve")
      .withHttpHeaders(tokenHeader())
      .post(EmptyBody)
      .flatMap { res =>
        if (res.status == Status.CREATED) Future.unit
        else Future.failed(parseErrorMessage(res))
      }

  def merge(id: MergeRequestId): Future[MergeRequestStatus] =
    ws.url(s"$mergeRequestUrl/${id.value}/merge")
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        "squash" -> true.toString,
        "should_remove_source_branch" -> true.toString
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
        "state" -> "opened"
      )
      .get()
      .flatMap { res =>
        if (res.status == Status.OK)
          res.json match {
            case JsArray(xs) => Future.successful(xs.nonEmpty)
            case other =>
              Future.failed(
                new Throwable(
                  s"expected result to be an json array, but was $other"
                )
              )
          }
        else Future.failed(parseErrorMessage(res))
      }

  def get(mergeRequestId: MergeRequestId) =
    ws
      .url(s"${this.mergeRequestUrl}/${mergeRequestId.value}")
      .withHttpHeaders(tokenHeader())
      .get()
      .map(a => (a.status, a.json))

  private def mergeRequestUrl =
    s"${projectsUrl()}/merge_requests"

  private def closeUrl(id: MergeRequestId) =
    s"$mergeRequestUrl/${id.value}"
}
