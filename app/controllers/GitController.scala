package controllers

import java.time.LocalDateTime
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
import logging.AppEventLogger
import logging.CorrelationId
import logging.LogEvent
import logging.LogResult
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
      val correlationId = CorrelationId.random()
      val event         = "git.admin.sync_core_files"
      AppEventLogger.info(
        logger,
        LogEvent(
          event = event,
          result = LogResult.Started,
          correlationId = correlationId
        )
      )
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
        coreDataPublisher ! CoreDataPublisher.Handle(contents, correlationId)
        AppEventLogger.info(
          logger,
          LogEvent(
            event = event,
            result = LogResult.Succeeded,
            correlationId = correlationId,
            details = Map(
              "pathCount"    -> paths.size.toString,
              "contentCount" -> contents.size.toString
            )
          )
        )
        NoContent
      }).recoverWith {
        case NonFatal(e) =>
          AppEventLogger.error(
            logger,
            LogEvent(
              event = event,
              result = LogResult.Failed,
              correlationId = correlationId,
              errorCode = Some("git_admin_sync_core_failed")
            ),
            e
          )
          Future.failed(e)
      }
    }

  def updateModuleFiles() =
    auth.andThen(resolveUser).andThen(isAdmin).async { _ =>
      val correlationId = CorrelationId.random()
      val event         = "git.admin.sync_module_files"
      AppEventLogger.info(
        logger,
        LogEvent(
          event = event,
          result = LogResult.Started,
          correlationId = correlationId
        )
      )
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
        modulePublisher ! ModulePublisher.NotifySubscribers(modules, correlationId)
        AppEventLogger.info(
          logger,
          LogEvent(
            event = event,
            result = LogResult.Succeeded,
            correlationId = correlationId,
            details = Map(
              "pathCount"   -> paths.size.toString,
              "moduleCount" -> modules.size.toString
            )
          )
        )
        NoContent
      }).recoverWith {
        case NonFatal(e) =>
          AppEventLogger.error(
            logger,
            LogEvent(
              event = event,
              result = LogResult.Failed,
              correlationId = correlationId,
              errorCode = Some("git_admin_sync_modules_failed")
            ),
            e
          )
          Future.failed(e)
      }
    }
}
