package git.api

import git._
import models.core.Identity
import service.Print

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitCommitService @Inject() (
    apiService: GitCommitApiService,
    fileApiService: GitFileApiService,
    implicit val ctx: ExecutionContext
) {

  private def config = apiService.config

  def commit(
      branch: Branch,
      author: Identity.Person,
      message: String,
      moduleId: UUID,
      print: Print
  ): Future[CommitId] = {
    val filePath = GitFilePath(moduleId)(config)
    val action = fileApiService
      .fileExists(filePath, branch)
      .map(exists =>
        GitCommitAction(
          if (exists) GitCommitActionType.Update
          else GitCommitActionType.Create,
          filePath,
          print.value
        )
      )
    for {
      action <- action
      res <- apiService.commit(
        branch,
        author.email getOrElse config.defaultEmail,
        author.fullName,
        message,
        Seq(action)
      )
    } yield res
  }
}
