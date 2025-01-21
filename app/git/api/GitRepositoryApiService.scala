package git.api

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.api.GitRepositoryApiService.nextLinkParser
import git.Branch
import git.GitConfig
import git.GitFilePath
import parser.Parser.end
import parser.Parser.prefix
import parser.Parser.prefixTo
import parser.Parser.skipFirst
import parser.ParserOps.P0
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSResponse
import play.mvc.Http.Status

object GitRepositoryApiService {
  def linkParser =
    skipFirst(prefixTo("<"))
      .take(prefixTo(">"))
      .skip(prefixTo("\""))
      .zip(prefixTo("\""))
      .many(prefix(",").or(end))

  def nextLinkParser =
    linkParser.map(_.find(_._2 == "next").map(_._1))
}

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

  private def parseNextPaginationUrl(r: WSResponse): Option[String] =
    r.header("Link").flatMap(nextLinkParser.parse(_)._1.fold(_ => None, identity))

  private def parseFiles(js: JsValue): List[GitFilePath] =
    js.\\("path")
      .map(s => GitFilePath(s.validate[String].getOrElse(s.toString())))
      .toList

  private def treeUrl(path: String, branch: Branch) =
    s"${repositoryUrl()}/tree?path=$path&per_page=100&ref=${branch.value}"
}
