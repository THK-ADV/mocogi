package controllers

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
    gitConfig: GitConfig,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def updateCoreFiles() = Action.async { _ => // TODO permission handling
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

  def updateModuleFiles() = Action.async { _ => // TODO permission handling
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
