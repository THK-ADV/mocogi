package git.api

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.Branch
import git.CommitId
import git.CompareDiff
import git.GitFilePath
import git.GitRepositoryConfig
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.mvc.Http.Status

@Singleton
final class GitRepositoryService @Inject() (
    private val ws: WSClient,
    protected val apiConfig: GitApiConfig,
    private val repositoryConfig: GitRepositoryConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {

  /**
   * Compares the file trees of both revisions directly (`git diff from to`), ignoring their merge base, and returns one
   * entry per changed file.
   *
   * GitLab computes the comparison within a time limit. If it is exceeded, GitLab still answers with HTTP 200 but sets
   * `compare_timeout` and returns only the files compared so far. Callers derive which modules differ from this list,
   * so a partial list would silently hide changes. Such a response fails with [[IncompleteComparisonException]]. With
   * `compare_timeout = false` the list of files is complete, although GitLab may still omit the text diff of single
   * large files (empty `diff`).
   *
   * If comparisons regularly become too large, the changed files should instead be derived from the paginated trees
   * (`repository/tree`) of both revisions by comparing the blob IDs of their module files, loading the text diff of a
   * module only on demand.
   */
  def compare(from: CommitId, to: CommitId): Future[List[CompareDiff]] =
    ws.url(s"${repositoryUrl()}/compare")
      .withHttpHeaders(tokenHeader())
      .withQueryStringParameters("from" -> from.value, "to" -> to.value, "straight" -> "true")
      .get()
      .flatMap { r =>
        if r.status != Status.OK then Future.failed(parseErrorMessage(r))
        else if (r.json \ "compare_timeout").asOpt[Boolean].contains(true) then
          Future.failed(IncompleteComparisonException())
        else Future.successful((r.json \ "diffs").as[List[CompareDiff]])
      }

  def listModuleFiles(branch: Branch): Future[List[GitFilePath]] =
    listFileNames(treeUrl(repositoryConfig.modulesPath, branch))

  private def listFileNames(url: String): Future[List[GitFilePath]] =
    ws
      .url(url)
      .withHttpHeaders(tokenHeader())
      .get()
      .flatMap { r =>
        if r.status != Status.OK then Future.failed(parseErrorMessage(r))
        else
          val files = parseFiles(r.json)
          parseNextPaginationUrl(r) match
            case Some(nextUrl) => listFileNames(nextUrl).map(files ::: _)
            case None          => Future.successful(files)
      }

  private def parseFiles(js: JsValue): List[GitFilePath] =
    js.\\("path")
      .map(s => GitFilePath(s.validate[String].getOrElse(s.toString())))
      .toList

  private def treeUrl(path: String, branch: Branch) =
    s"${repositoryUrl()}/tree?path=$path&per_page=100&ref=${branch.value}"
}
