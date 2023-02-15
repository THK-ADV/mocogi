/*package git.webhook

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

  def handlePushEvent(
      downloadActor: GitFilesDownloadActor
  )(implicit r: Request[JsValue]) =
    for {
      projectId <- parseProjectId
      changes <- parseChanges
    } yield {
      downloadActor.download(changes, projectId)
      "Okay!"
    }
}*/
