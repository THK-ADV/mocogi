package git.api

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.*
import models.core.Identity
import service.Print

@Singleton
final class GitCommitService @Inject() (
    apiService: GitCommitApiService,
    fileApiService: GitFileApiService,
    implicit val ctx: ExecutionContext
) {

  private implicit def config: GitConfig = apiService.config

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
        author.email.getOrElse(config.defaultEmail),
        author.fullName,
        message,
        Seq(action)
      )
    } yield res
  }

  /**
   * Returns the content for a module that has been modified in the given commit. This method assumes that there are
   * only module changes and a single diff.
   */
  def getLatestModuleFromCommit(
      sha: String,
      branch: Branch,
      module: UUID
  ): Future[Option[(GitFileContent, CommitDiff)]] =
    apiService.getCommitDiff(sha).map(_.collectFirst { case d if d.newPath.moduleId.contains(module) => d }).flatMap {
      case Some(c) => fileApiService.download(c.newPath, branch).map(_.map(_ -> c))
      case None    => Future.successful(None)
    }

  /**
   * Returns the content for all modules that has been modified in the given commit.
   */
  def getAllModulesFromCommit(sha: String, branch: Branch): Future[List[(GitFileContent, CommitDiff)]] =
    for
      commits <- apiService.getCommitDiff(sha)
      downloads <- Future.sequence(commits.collect {
        case cd if cd.newPath.isModule =>
          fileApiService.download(cd.newPath, branch).collect { case Some(c) => (c, cd) }
      })
    yield downloads
}
