package git

import play.api.libs.ws.{EmptyBody, WSClient}
import play.mvc.Http.Status

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitMergeRequestService @Inject() (
    private val ws: WSClient,
    val gitConfig: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {

  type MergeRequestID = Int

  def createMergeRequest(
      sourceBranch: String,
      username: String,
      description: String
  ): Future[MergeRequestID] =
    ws
      .url(this.mergeRequestUrl)
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        "source_branch" -> sourceBranch,
        "target_branch" -> gitConfig.mainBranch,
        "title" -> s"$username $currentDate",
        "description" -> description,
        "remove_source_branch" -> true.toString,
        "squash_on_merge" -> true.toString,
        "squash" -> true.toString
      )
      .post(EmptyBody)
      .flatMap { res =>
        if (res.status == Status.CREATED)
          Future.successful(res.json.\("iid").validate[Int].get)
        else Future.failed(parseErrorMessage(res))
      }

  def deleteMergeRequest(id: MergeRequestID): Future[Unit] =
    ws
      .url(this.deleteRequest(id))
      .withHttpHeaders(tokenHeader())
      .delete()
      .flatMap { res =>
        if (res.status == Status.NO_CONTENT) Future.unit
        else Future.failed(parseErrorMessage(res))
      }

  private def currentDate = {
    val now = LocalDate.now
    val pattern = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    now.format(pattern)
  }

  private def mergeRequestUrl =
    s"${projectsUrl()}/merge_requests"

  private def deleteRequest(id: MergeRequestID) =
    s"$mergeRequestUrl/$id"
}
