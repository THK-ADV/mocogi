package controllers

import auth.AuthorizationAction
import controllers.actions.{AdminCheck, PermissionCheck}
import git.api.{GitFileDownloadService, GitRepositoryApiService}
import git.publisher.{CoreDataPublisher, ModulePublisher}
import git.{GitChanges, GitConfig}
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitController @Inject() (
    cc: ControllerComponents,
    downloadService: GitFileDownloadService,
    gitRepositoryApiService: GitRepositoryApiService,
    modulePublisher: ModulePublisher,
    coreDataPublisher: CoreDataPublisher,
    auth: AuthorizationAction,
    gitConfig: GitConfig,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AdminCheck
    with PermissionCheck {

  def updateCoreFiles() =
    auth andThen isAdmin async { _ =>
      for {
        paths <- gitRepositoryApiService.listCoreFiles()
        contents <- Future.sequence(
          paths.map(path =>
            downloadService
              .downloadFileContent(path, gitConfig.mainBranch)
              .collect { case Some(content) =>
                path -> content
              }
          )
        )
      } yield {
        coreDataPublisher.notifySubscribers(GitChanges(contents))
        NoContent
      }
    }

  def updateModuleFiles() =
    auth andThen isAdmin async { _ =>
      for {
        paths <- gitRepositoryApiService.listModuleFiles()
        contents <- Future.sequence(
          paths.map(path =>
            downloadService
              .downloadFileContent(path, gitConfig.mainBranch)
              .collect { case Some(content) =>
                path -> content
              }
          )
        )
      } yield {
        modulePublisher.notifySubscribers(GitChanges(contents))
        NoContent
      }
    }
}
