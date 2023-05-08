package git.webhook

import git.publisher.GitFilesDownloadActor
import git.{GitChanges, GitFilePath}
import play.api.libs.json.{JsArray, JsResult, JsValue}
import play.api.mvc.Request

import java.time.LocalDateTime

object GitPushEventHandler {
  private def parseProjectId(implicit r: Request[JsValue]): JsResult[Int] =
    r.body.\("project_id").validate[Int]

  private def parseChanges(implicit
      r: Request[JsValue]
  ): JsResult[GitChanges[List[GitFilePath]]] =
    for {
      commits <- r.body.\("commits").validate[JsArray]
      last = commits.last
      added <- last.\("added").validate[List[String]]
      modified <- last.\("modified").validate[List[String]]
      removed <- last.\("removed").validate[List[String]]
      commitId <- last.\("id").validate[String]
      timestamp <- last.\("timestamp").validate[LocalDateTime]
    } yield GitChanges(
      added.map(GitFilePath.apply),
      modified.map(GitFilePath.apply),
      removed.map(GitFilePath.apply),
      commitId,
      timestamp
    )

  def removeModuleChanges(
      changes: GitChanges[List[GitFilePath]],
      modulesRootFolder: String
  ): GitChanges[List[GitFilePath]] = changes.copy(
    added = changes.added.filterNot(_.value.startsWith(modulesRootFolder)),
    modified =
      changes.modified.filterNot(_.value.startsWith(modulesRootFolder)),
    removed = changes.removed.filterNot(_.value.startsWith(modulesRootFolder))
  )

  def handlePushEvent(
      downloadActor: GitFilesDownloadActor,
      moduleMode: Boolean,
      modulesRootFolder: String
  )(implicit r: Request[JsValue]) =
    for {
      projectId <- parseProjectId
      allChanges <- parseChanges
    } yield {
      val changes =
        if (moduleMode) allChanges
        else removeModuleChanges(allChanges, modulesRootFolder)
      downloadActor.download(changes, projectId)
      "Okay!"
    }
}
