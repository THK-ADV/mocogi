package git.api

import git.GitConfig
import models.Branch
import play.api.libs.ws.{EmptyBody, WSClient}
import play.mvc.Http.Status

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/*
  One branch per module
  Branch naming convention: module id
 */
@Singleton
final class GitBranchApiService @Inject() (
    private val ws: WSClient,
    val config: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {

  def createBranch(branch: Branch): Future[Unit] = {
    ws
      .url(this.branchUrl())
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        ("branch", branch.value),
        ("ref", config.mainBranch)
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
        if (res.status == Status.NO_CONTENT) Future.unit
        else Future.failed(parseErrorMessage(res))
      }

  private def branchUrl() =
    s"${repositoryUrl()}/branches"
}
