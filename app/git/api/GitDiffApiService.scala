package git.api

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.Branch
import git.Diff
import git.GitConfig
import play.api.libs.ws.WSClient

@Singleton
final class GitDiffApiService @Inject() (
    val config: GitConfig,
    private val ws: WSClient,
    private implicit val ctx: ExecutionContext
) extends GitService {

  def compare(from: Branch, to: Branch): Future[List[Diff]] =
    ws
      .url(s"${projectsUrl()}/repository/compare")
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters(
        ("from", from.value),
        ("to", to.value)
      )
      .get()
      .flatMap { res =>
        if (res.status == 200)
          Future.successful(
            res.json.\("diffs").validate[List[Diff]].getOrElse(Nil)
          )
        else Future.failed(parseErrorMessage(res))
      }
}
