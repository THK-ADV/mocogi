package git.api

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.Branch
import git.GitConfig
import git.GitFilePath
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.mvc.Http.Status

@Singleton
final class GitRepositoryApiService @Inject() (
    private val ws: WSClient,
    val config: GitConfig,
    private implicit val ctx: ExecutionContext
) extends GitService {

  def listCoreFiles(branch: Branch): Future[List[GitFilePath]] =
    listFileNames(treeUrl(config.coreFolder, branch))

  def listModuleFiles(branch: Branch): Future[List[GitFilePath]] =
    listFileNames(treeUrl(config.modulesFolder, branch))

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
