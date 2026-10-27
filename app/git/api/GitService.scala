package git.api

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import scala.util.control.NonFatal

import git.GitFilePath
import parser.ParserOps.P0
import play.api.libs.ws.WSResponse
import settings.SecretString

private[api] trait GitService {
  import GitService.nextLinkParser

  protected def apiConfig: GitApiConfig

  protected def projectsUrl() = s"${apiConfig.baseUrl}/projects/${apiConfig.projectId}"

  protected def repositoryUrl() = s"${projectsUrl()}/repository"

  protected def tokenHeader() = ("PRIVATE-TOKEN", SecretString.unwrap(apiConfig.accessToken))

  protected def parseErrorMessage(res: WSResponse): GitApiException = {
    val message =
      try (res.json \ "message").asOpt[String].getOrElse(res.body)
      catch { case NonFatal(_) => res.body }
    val detail = message.replaceAll("\\s+", " ").trim.take(512)
    new GitApiException(res.status, if detail.nonEmpty then detail else res.statusText)
  }

  protected def parseNextPaginationUrl(r: WSResponse): Option[String] =
    r.header("Link").flatMap(nextLinkParser.parse(_)._1.fold(_ => None, identity))

  protected def urlEncoded(path: GitFilePath) = URLEncoder.encode(path.value, StandardCharsets.UTF_8)
}

object GitService {
  import parser.Parser.end
  import parser.Parser.prefix
  import parser.Parser.prefixTo
  import parser.Parser.skipFirst

  def linkParser =
    skipFirst(prefixTo("<"))
      .take(prefixTo(">"))
      .skip(prefixTo("\""))
      .zip(prefixTo("\""))
      .many(prefix(",").or(end))

  def nextLinkParser = linkParser.map(_.find(_._2 == "next").map(_._1))
}
