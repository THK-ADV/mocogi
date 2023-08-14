package git.api

import git.GitConfig
import play.api.libs.ws.WSClient
import play.mvc.Http.Status

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitBranchApiService @Inject() (
    private val ws: WSClient,
    val gitConfig: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {

  def createBranch(user: String): Future[String] = {
    ???
//    val branchName = this.branchName(user)
//    ws
//      .url(this.branchUrl())
//      .withHttpHeaders(tokenHeader())
//      .withQueryStringParameters(
//        ("branch", branchName),
//        ("ref", gitConfig.mainBranch)
//      )
//      .post(EmptyBody)
//      .flatMap { res =>
//        if (res.status == Status.CREATED)
//          Future.successful(UserBranch(user, branchName, None, None))
//        else Future.failed(parseErrorMessage(res))
//      }
  }

  def deleteBranch(user: String) = {
    val branchName = this.branchName(user)
    ws
      .url(s"${this.branchUrl()}/$branchName")
      .withHttpHeaders(tokenHeader())
      .delete()
      .flatMap { res =>
        if (res.status == Status.NO_CONTENT) Future.successful(branchName)
        else Future.failed(parseErrorMessage(res))
      }
  }

  private def branchName(user: String) =
    user

  private def branchUrl() =
    s"${repositoryUrl()}/branches"
}
