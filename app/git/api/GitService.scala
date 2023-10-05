package git.api

import git.GitConfig
import play.api.libs.ws.WSResponse

trait GitService {
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
        errs => new Throwable(errs.mkString("\n")),
        msg => new Throwable(msg)
      )
}
