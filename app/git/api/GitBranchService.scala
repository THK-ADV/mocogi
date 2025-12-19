package git.api

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.Branch
import git.GitConfig
import play.api.libs.ws.writeableOf_WsBody
import play.api.libs.ws.EmptyBody
import play.api.libs.ws.WSClient
import play.mvc.Http.Status

/**
 * Invariance: one branch per module.
 * Naming convention: module ID.
 */
@Singleton
final class GitBranchService @Inject() (
    private val ws: WSClient,
    val config: GitConfig,
    implicit val ctx: ExecutionContext
) extends GitService {

  def createModuleBranch(moduleId: UUID): Future[Branch] = {
    val branch = createBranch(moduleId)
    createBranch(branch, config.draftBranch).map(_ => branch)
  }

  def deleteModuleBranch(moduleId: UUID): Future[Unit] = {
    val branch = createBranch(moduleId)
    deleteBranch(branch)
  }

  private def createBranch(module: UUID): Branch =
    Branch(module.toString)

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

  private def deleteBranch(branch: Branch): Future[Unit] =
    ws
      .url(s"${this.branchUrl()}/${branch.value}")
      .withHttpHeaders(tokenHeader())
      .delete()
      .flatMap { res =>
        if (res.status == Status.NO_CONTENT) Future.unit
        else Future.failed(parseErrorMessage(res))
      }

  private def branchUrl() =
    s"${repositoryUrl()}/branches"
}
