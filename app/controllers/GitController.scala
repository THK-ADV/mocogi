package controllers

import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import auth.AuthorizationAction
import auth.Role.Admin
import controllers.actions.RoleCheck
import git.api.GitCommitService
import git.api.GitFileDownloadService
import git.api.GitRepositoryApiService
import git.publisher.CoreDataPublisher
import git.publisher.ModulePublisher
import git.GitConfig
import git.GitFile
import git.GitFileStatus
import play.api.cache.Cached
import play.api.libs.json.JsNull
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents

@Singleton
final class GitController @Inject() (
    cc: ControllerComponents,
    downloadService: GitFileDownloadService,
    gitRepositoryApiService: GitRepositoryApiService,
    gitCommitService: GitCommitService,
    modulePublisher: ModulePublisher,
    coreDataPublisher: CoreDataPublisher,
    auth: AuthorizationAction,
    gitConfig: GitConfig,
    cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with RoleCheck {

  def latestModuleUpdate() =
    cached.status(r => r.method + r.uri, 200, 30.minutes) {
      Action.async(_ =>
        gitCommitService
          .getLatestCommitDateOfModulesFolder()
          .map(d => Ok(d.fold(JsNull)(Json.toJson)))
          .recover { case NonFatal(_) => Ok(JsNull) }
      )
    }

  def updateCoreFiles() =
    auth.andThen(hasRole(Admin)).async { _ =>
      for {
        paths <- gitRepositoryApiService.listCoreFiles(gitConfig.mainBranch)
        contents <- Future.sequence(
          paths.map(path =>
            downloadService
              .downloadFileContent(path, gitConfig.mainBranch)
              .collect { case Some(content) => (GitFile.CoreFile(path, GitFileStatus.Modified), content) }
          )
        )
      } yield {
        coreDataPublisher.notifySubscribers(contents)
        NoContent
      }
    }

  def updateModuleFiles() =
    auth.andThen(hasRole(Admin)).async { _ =>
      for {
        paths <- gitRepositoryApiService.listModuleFiles(gitConfig.mainBranch)
        modules <- Future.sequence(
          paths.par.collect {
            case path if path.isModule(gitConfig) =>
              downloadService
                .downloadFileContentWithLastModified(path, gitConfig.mainBranch)
                .collect {
                  case Some((content, Some(lastModified))) =>
                    val moduleId = path.moduleId(gitConfig)
                    assume(moduleId.isDefined, s"expected module id for ${path.value}")
                    (
                      GitFile.ModuleFile(
                        path,
                        path.moduleId(gitConfig).get,
                        GitFileStatus.Modified,
                        lastModified
                      ),
                      content
                    )
                }
          }.toList
        )
      } yield {
        modulePublisher.notifySubscribers(modules)
        NoContent
      }
    }
}
