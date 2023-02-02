package git

import play.api.libs.ws.WSResponse

trait GitService {
  def gitConfig: GitConfig

  def projectsUrl() =
    s"${gitConfig.baseUrl}/projects/${gitConfig.projectId}"

  def repositoryUrl() =
    s"${projectsUrl()}/repository"

  def tokenHeader() =
    ("PRIVATE-TOKEN", gitConfig.accessToken)

  def parseErrorMessage(res: WSResponse) =
    res.json
      .\("message")
      .validate[String]
      .fold(
        errs => new Throwable(errs.mkString("\n")),
        msg => new Throwable(msg)
      )
}
