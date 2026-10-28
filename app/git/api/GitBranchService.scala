package git.api

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.Branch
import git.CommitId
import git.GitRepositoryConfig
import play.api.libs.json.JsObject
import play.api.libs.ws.writeableOf_WsBody
import play.api.libs.ws.EmptyBody
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSRequest
import play.mvc.Http.Status

/**
 * Invariance: one branch per module.
 * Naming convention: module ID.
 */
@Singleton
final class GitBranchService @Inject() (
    private val ws: WSClient,
    protected val apiConfig: GitApiConfig,
    private val repositoryConfig: GitRepositoryConfig,
    implicit val ctx: ExecutionContext
) extends GitService {

  /** Returns the latest commit of `branch`. A missing branch fails with a [[GitApiException]] with status 404. */
  def head(branch: Branch): Future[CommitId] =
    ws.url(s"${branchUrl()}/${branch.value}")
      .withHttpHeaders(tokenHeader())
      .get()
      .flatMap { res =>
        if res.status == Status.OK then Future.successful(CommitId((res.json \ "commit" \ "id").as[String]))
        else Future.failed(parseErrorMessage(res))
      }

  /** Fetches existing UUID-named GitLab branches across all pages, regardless of merge status. */
  def listModuleBranches(): Future[List[Branch]] = {
    def go(request: WSRequest): Future[List[Branch]] =
      request
        .withHttpHeaders(tokenHeader())
        .get()
        .flatMap { res =>
          if res.status != Status.OK then Future.failed(parseErrorMessage(res))
          else
            val branches = res.json.as[List[JsObject]].map(js => Branch((js \ "name").as[String]))
            parseNextPaginationUrl(res) match
              case Some(nextUrl) => go(ws.url(nextUrl)).map(branches ::: _)
              case None          => Future.successful(branches)
        }

    go(
      ws.url(branchUrl())
        .withQueryStringParameters(
          "regex"    -> "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
          "per_page" -> "100"
        )
    )
  }

  def createModuleBranch(moduleId: UUID): Future[Branch] = {
    val branch = createBranch(moduleId)
    createBranch(branch, repositoryConfig.previewBranch).map(_ => branch)
  }

  /** Deletes the module branch. A missing branch counts as deleted. */
  def deleteModuleBranch(moduleId: UUID): Future[Unit] = {
    val branch = createBranch(moduleId)
    deleteBranch(branch)
  }

  private def createBranch(module: UUID): Branch = Branch(module.toString)

  private def createBranch(branch: Branch, source: Branch): Future[Unit] = {
    ws
      .url(this.branchUrl())
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        ("branch", branch.value),
        ("ref", source.value)
      )
      .post(EmptyBody)
      .flatMap { res =>
        if (res.status == Status.CREATED) Future.unit
        else Future.failed(parseErrorMessage(res))
      }
  }

  def deleteBranch(branch: Branch): Future[Unit] =
    ws
      .url(s"${this.branchUrl()}/${branch.value}")
      .withHttpHeaders(tokenHeader())
      .delete()
      .flatMap { res =>
        if (res.status == Status.NO_CONTENT || res.status == Status.NOT_FOUND) Future.unit
        else Future.failed(parseErrorMessage(res))
      }

  private def branchUrl() = s"${repositoryUrl()}/branches"
}
