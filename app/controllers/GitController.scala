package controllers

import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import controllers.actions.AdminCheck
import controllers.actions.PermissionCheck
import git.api.GitFileDownloadService
import git.api.GitRepositoryApiService
import git.publisher.CoreDataPublisher
import git.publisher.ModulePublisher
import git.GitConfig
import git.GitFile
import git.GitFileStatus
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents

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
    auth.andThen(isAdmin).async { _ =>
      for {
        paths <- gitRepositoryApiService.listCoreFiles()
        contents <- Future.sequence(
          paths.map(path =>
            downloadService
              .downloadFileContent(path, gitConfig.mainBranch)
              .collect {
                case Some(content) =>
                  (GitFile.CoreFile(path, GitFileStatus.Modified), content)
              }
          )
        )
      } yield {
        coreDataPublisher.notifySubscribers(contents)
        NoContent
      }
    }

  def updateModuleFiles() =
    auth.andThen(isAdmin).async { _ =>
      for {
        paths <- gitRepositoryApiService.listModuleFiles()
        modules <- Future.sequence(
          paths.collect {
            case path if path.isModule(gitConfig) =>
              downloadService
                .downloadFileContent(path, gitConfig.mainBranch)
                .collect {
                  case Some(content) =>
                    (
                      GitFile.ModuleFile(
                        path,
                        path.moduleId(gitConfig).get,
                        GitFileStatus.Modified
                      ),
                      content
                    )
                }
          }
        )
      } yield {
        // unable to retrieve the real last modified date
        modulePublisher.notifySubscribers(modules, LocalDateTime.now())
        NoContent
      }
    }
}
