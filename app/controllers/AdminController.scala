package controllers

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import controllers.actions.UserResolveAction
import database.repo.ModuleDeletionRepository
import database.repo.PermissionRepository
import git.api.GitFileService
import git.GitConfig
import git.GitFilePath
import models.CreatedModule
import parsing.RawModuleParser
import permission.AdminCheck
import play.api.libs.json.Reads
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.api.Logging
import service.ModuleCreationService
import database.repo.schedule.ScheduleEntryBootstrapRepository
import controllers.actions.UserRequest
import play.api.libs.json.JsArray
import play.api.libs.json.JsValue

final class AdminController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    moduleDeletionRepository: ModuleDeletionRepository,
    moduleCreationService: ModuleCreationService,
    downloadService: GitFileService,
    val permissionRepository: PermissionRepository,
    val scheduleEntryBootstrapRepo: ScheduleEntryBootstrapRepository,
    implicit val gitConfig: GitConfig,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AdminCheck
    with UserResolveAction
    with Logging {

  def deleteModule(module: UUID) =
    auth.andThen(resolveUser).andThen(isAdmin).async { _ =>
      logger.info(s"deleting module $module ...")
      moduleDeletionRepository.delete(module).map(_ => NoContent)
    }

  def createNewModulesFromDraftBranch() =
    auth.andThen(resolveUser).andThen(isAdmin).async(parse.json[List[String]]) { r =>
      val moduleIds = r.body
      for
        modulesToCreate <- Future.sequence(
          moduleIds.map(id =>
            downloadService.downloadFileContent(GitFilePath(UUID.fromString(id)), gitConfig.draftBranch).collect {
              case Some(c) => RawModuleParser.parseCreatedModuleInformation(c.value)
            }
          )
        )
        _ <- moduleCreationService.createManyWithPermissions(modulesToCreate)
      yield {
        logger.info(s"created ${modulesToCreate.size} new modules")
        NoContent
      }
    }

  /** Bootstraps schedule entries from raw JSON imported from external data. */
  def bootstrapScheduleEntries() =
    auth(parse.json).andThen(resolveUser).andThen(isAdmin).async { (r: UserRequest[JsValue]) =>
      val json = r.body.validate[JsArray].get.value
      scheduleEntryBootstrapRepo.createFromJson(json.toVector).map(_ => NoContent)
    }

  /** Recreates module–teaching-unit associations based on the modules' PO relations. */
  def recreateModuleTeachingUnits() =
    auth.andThen(resolveUser).andThen(isAdmin).async { _ =>
      scheduleEntryBootstrapRepo.bootstrapModuleTeachingUnit().map(_ => NoContent)
    }
}
