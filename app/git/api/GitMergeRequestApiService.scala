package git.api

import git.GitConfig
import models.{Branch, MergeRequestId, MergeRequestStatus}
import play.api.libs.ws.{EmptyBody, WSClient}
import play.mvc.Http.Status

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
      labels: List[String]
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
        "labels" -> labels.mkString(",")
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

  def canBeMerged(id: MergeRequestId): Future[Unit] = {
    def go(trys: Int): Future[Unit] =
      ws
        .url(s"${this.mergeRequestUrl}/${id.value}")
        .withHttpHeaders(tokenHeader())
        .get()
        .flatMap { res =>
          if (res.status != Status.OK)
            Future.failed(parseErrorMessage(res))
          else {
            val mergeStatus = res.json.\("merge_status").validate[String]
            val canBeMerged =
              mergeStatus.map(_ == "can_be_merged").getOrElse(false)
            if (trys <= 0)
              Future.failed(
                new Throwable(
                  "timeout trying to check merge status to be mergeable"
                )
              )
            else {
              if (canBeMerged) Future.unit
              else go(trys - 1)
            }
          }
        }
    go(15)
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

  private def mergeRequestUrl =
    s"${projectsUrl()}/merge_requests"

  private def closeUrl(id: MergeRequestId) =
    s"$mergeRequestUrl/${id.value}"
}
