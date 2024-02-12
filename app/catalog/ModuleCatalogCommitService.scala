package catalog

import git.api.{GitCommitApiService, GitFileApiService}
import git.{GitCommitAction, GitCommitActionType, GitFilePath}
import models.{Branch, Semester}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleCatalogCommitService @Inject() (
    commitApiService: GitCommitApiService,
    fileApiService: GitFileApiService,
    private implicit val ctx: ExecutionContext
) {

  private def config = commitApiService.config

  type Content = String

  def commit(
      files: List[(GitFilePath, Content)],
      semester: Semester,
      branch: Branch
  ) = {
    val commitActions = Future
      .sequence(
        files.map(f => fileApiService.fileExists(f._1, branch).map(_ -> f))
      )
      .map(_.map { case (exists, (path, content)) =>
        val actionType =
          if (exists) GitCommitActionType.Update else GitCommitActionType.Create
        GitCommitAction(actionType, path, content)
      })

    for {
      actions <- commitActions
      _ <- commitApiService
        .commit(
          branch,
          config.defaultEmail,
          config.defaultUser,
          s"adds module and electives catalogs for ${semester.deLabel} ${semester.year}",
          actions
        )
    } yield ()
  }

}
