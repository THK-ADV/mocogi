package controllers

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import auth.Role.Admin
import controllers.actions.PermissionCheck
import controllers.actions.RoleCheck
import database.repo.ModuleDeletionRepository
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.api.Logging
import validation.ModuleExaminationValidator

final class AdminController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    moduleExaminationValidator: ModuleExaminationValidator,
    repo: ModuleDeletionRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with RoleCheck
    with PermissionCheck
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
      repo.delete(module).map(_ => NoContent)
    }
}
