package controllers

import git.api.{GitFileDownloadService, GitRepositoryApiService}
import git.publisher.{CoreDataPublisher, ModuleCompendiumPublisher}
import git.{GitChanges, GitConfig}
import models.Branch
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitController @Inject() (
    cc: ControllerComponents,
    downloadService: GitFileDownloadService,
    gitRepositoryApiService: GitRepositoryApiService,
    compendiumPublisher: ModuleCompendiumPublisher,
    coreDataPublisher: CoreDataPublisher,
    gitConfig: GitConfig,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def updateCoreFiles() = Action.async { _ => // TODO permission handling
    val mainBranch = Branch(gitConfig.mainBranch)
    for {
      paths <- gitRepositoryApiService.listCoreFiles()
      contents <- Future.sequence(
        paths.map(path =>
          downloadService.downloadFileContent(path, mainBranch).collect {
            case Some(content) => path -> content
          }
        )
      )
    } yield {
      coreDataPublisher.notifySubscribers(GitChanges(contents))
      NoContent
    }
  }

  def updateModuleFiles() = Action.async { _ => // TODO permission handling
    val mainBranch = Branch(gitConfig.mainBranch)
    for {
      paths <- gitRepositoryApiService.listModuleFiles()
      contents <- Future.sequence(
        paths.map(path =>
          downloadService.downloadFileContent(path, mainBranch).collect {
            case Some(content) => path -> content
          }
        )
      )
    } yield {
      compendiumPublisher.notifySubscribers(GitChanges(contents))
      NoContent
    }
  }
}
