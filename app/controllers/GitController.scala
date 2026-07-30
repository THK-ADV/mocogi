package controllers

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import auth.AuthorizationAction
import controllers.actions.UserResolveAction
import database.repo.PermissionRepository
import git.api.GitCommitService
import git.api.GitFileService
import git.api.GitRepositoryService
import git.publisher.CoreDataPublisher
import git.publisher.ModulePublisher
import git.GitConfig
import git.GitFile
import git.GitFileStatus
import logging.errorC
import logging.infoC
import logging.CorrelationId
import org.apache.pekko.actor.ActorRef
import permission.AdminCheck
import play.api.cache.Cached
import play.api.libs.json.JsNull
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.api.Logging
import security.ClientErrorResponse

@Singleton
final class GitController @Inject() (
    cc: ControllerComponents,
    downloadService: GitFileService,
    gitRepositoryApiService: GitRepositoryService,
    gitCommitService: GitCommitService,
    @Named("ModulePublisher") modulePublisher: ActorRef,
    @Named("CoreDataPublisher") coreDataPublisher: ActorRef,
    auth: AuthorizationAction,
    gitConfig: GitConfig,
    cached: Cached,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AdminCheck
    with UserResolveAction
    with Logging {

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
    auth.andThen(resolveUser).andThen(isAdmin).async { _ =>
      given CorrelationId = CorrelationId.random()
      (for {
        paths    <- gitRepositoryApiService.listCoreFiles(gitConfig.mainBranch)
        contents <- Future.sequence(
          paths.map(path =>
            downloadService
              .downloadFileContent(path, gitConfig.mainBranch)
              .collect { case Some(content) => (GitFile.CoreFile(path, GitFileStatus.Modified), content) }
          )
        )
      } yield {
        coreDataPublisher ! CoreDataPublisher.Handle(contents, summon[CorrelationId])
        logger.infoC(s"admin sync core files ok paths=${paths.size} contents=${contents.size}")
        NoContent
      }).recoverWith {
        case NonFatal(e) =>
          logger.errorC("admin sync core files failed", e)
          Future.failed(e)
      }
    }

  def updateModuleFiles() =
    auth.andThen(resolveUser).andThen(isAdmin).async { _ =>
      given CorrelationId = CorrelationId.random()
      (for {
        paths   <- gitRepositoryApiService.listModuleFiles(gitConfig.mainBranch)
        modules <- Future.sequence(
          paths.par.collect {
            case path if path.isModule(gitConfig) =>
              downloadService
                .download(path, gitConfig.mainBranch)
                .flatMap {
                  case Some((content, Some(commit))) =>
                    gitCommitService.getCommitDate(commit.value).map(d => Some(content, d))
                  case _ => Future.successful(None)
                }
                .collect {
                  case Some((content, lastModified)) =>
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
        modulePublisher ! ModulePublisher.NotifySubscribers(modules, summon[CorrelationId])
        logger.infoC(s"admin sync module files ok paths=${paths.size} modules=${modules.size}")
        NoContent
      }).recoverWith {
        case NonFatal(e) =>
          logger.errorC("admin sync module files failed", e)
          Future.failed(e)
      }
    }
}
