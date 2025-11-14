package controllers

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import auth.Role.Admin
import controllers.actions.RoleCheck
import database.repo.ModuleDeletionRepository
import git.api.GitFileDownloadService
import git.GitConfig
import git.GitFilePath
import models.CreatedModule
import parsing.RawModuleParser
import play.api.libs.json.Reads
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.api.Logging
import service.ModuleCreationService
import validation.ModuleExaminationValidator

final class AdminController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    moduleExaminationValidator: ModuleExaminationValidator,
    moduleDeletionRepository: ModuleDeletionRepository,
    moduleCreationService: ModuleCreationService,
    downloadService: GitFileDownloadService,
    implicit val gitConfig: GitConfig,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with RoleCheck
    with Logging {

  def invalidModuleExams() =
    auth.andThen(hasRole(Admin)).async { _ =>
      moduleExaminationValidator.getAllInvalidModuleExams.map { xs =>
        xs.sortBy(_._1.id)
          .foreach(a => println(s"${a._1};${a._2.mkString("[", ",", s"];${a._3.mkString("{", ",", "}")}")}"))
        NoContent
      }
    }

  def deleteModule(module: UUID) =
    auth.andThen(hasRole(Admin)).async { _ =>
      logger.info(s"deleting module $module ...")
      moduleDeletionRepository.delete(module).map(_ => NoContent)
    }

  def createNewModulesFromDraftBranch() =
    auth.andThen(hasRole(Admin)).async(parse.json[List[String]]) { r =>
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
}
