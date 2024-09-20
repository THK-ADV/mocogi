package git.api

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import git.Branch
import git.GitConfig
import git.GitFilePath
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
        errs => new Exception(errs.mkString("\n")),
        msg => new Exception(msg)
      )

  private def urlEncoded(path: GitFilePath) =
    URLEncoder.encode(path.value, StandardCharsets.UTF_8)

  def fileUrl(path: GitFilePath, branch: Branch) =
    s"${config.baseUrl}/projects/${config.projectId}/repository/files/${urlEncoded(path)}/raw?ref=${branch.value}"
}
