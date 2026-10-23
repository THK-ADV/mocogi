package git.api

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.Branch
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

  def listModuleFiles(branch: Branch): Future[List[GitFilePath]] =
    listFileNames(treeUrl(repositoryConfig.modulesPath, branch))

  private def listFileNames(url: String): Future[List[GitFilePath]] =
    ws
      .url(url)
      .withHttpHeaders(tokenHeader())
      .get()
      .flatMap { r =>
        if r.status != Status.OK then Future.failed(new Exception(r.toString))
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
