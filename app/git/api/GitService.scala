package git.api

import git.GitConfig
import parser.ParserOps.P0
import play.api.libs.ws.WSResponse

trait GitService {
  import GitService.nextLinkParser

  def config: GitConfig

  def projectsUrl() =
    s"${config.baseUrl}/projects/${config.projectId}"

  def repositoryUrl() =
    s"${projectsUrl()}/repository"

  def tokenHeader() =
    ("PRIVATE-TOKEN", config.accessToken)

  def parseErrorMessage(res: WSResponse) =
    res.json
      .\("message")
      .validate[String]
      .fold(
        errs => new Exception(errs.mkString("\n")),
        msg => new Exception(msg)
      )

  def parseNextPaginationUrl(r: WSResponse): Option[String] =
    r.header("Link").flatMap(nextLinkParser.parse(_)._1.fold(_ => None, identity))
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

  def nextLinkParser =
    linkParser.map(_.find(_._2 == "next").map(_._1))
}
