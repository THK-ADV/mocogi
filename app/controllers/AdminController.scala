package controllers

import javax.inject.Inject

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import auth.Role.Admin
import controllers.actions.PermissionCheck
import controllers.actions.RoleCheck
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import validation.ModuleExaminationValidator

final class AdminController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    moduleExaminationValidator: ModuleExaminationValidator,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with RoleCheck
    with PermissionCheck {

  def invalidModuleExams =
    auth.andThen(hasRole(Admin)).async { _ =>
      moduleExaminationValidator.getAllInvalidModuleExams.map { xs =>
        xs.sortBy(_._1.id)
          .foreach(a => println(s"${a._1};${a._2.mkString("[", ",", s"];${a._3.mkString("{", ",", "}")}")}"))
        NoContent
      }
    }
}
