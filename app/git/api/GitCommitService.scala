package git.api

import git.{Branch, CommitId, GitCommitAction, GitCommitActionType, GitFilePath}
import models._
import models.core.Identity
import service.Print

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitCommitService @Inject() (
    apiService: GitCommitApiService,
    implicit val ctx: ExecutionContext
) {
  def commit(
      branch: Branch,
      author: Identity.Person,
      message: String,
      moduleId: UUID,
      status: ModuleDraftSource,
      print: Print
  ): Future[CommitId] = {
    val gitActionType = status match {
      case ModuleDraftSource.Added    => GitCommitActionType.Create
      case ModuleDraftSource.Modified => GitCommitActionType.Update
    }
    val filePath = GitFilePath(moduleId)(apiService.config)
    val action = GitCommitAction(gitActionType, filePath, print.value)
    apiService.commit(
      branch,
      author.email getOrElse apiService.config.defaultEmail,
      author.fullName,
      message,
      Seq(action)
    )
  }
}
