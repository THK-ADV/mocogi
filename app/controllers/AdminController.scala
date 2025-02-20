package controllers

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import auth.AuthorizationAction
import auth.Role.Admin
import controllers.actions.PermissionCheck
import controllers.actions.RoleCheck
import database.repo.ModuleDeletionRepository
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
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

  def deleteModules() =
    auth.andThen(hasRole(Admin)).apply { (_: Request[AnyContent]) =>
      val modulesToDelete = List(
        "66cdf64a-a164-46b1-a654-0c95564b563c",
        "e399b63c-64d4-403f-9275-75c5e1e1359b",
        "09c692da-e860-4272-8279-263bbdb94c82",
        "0df7ec6b-747c-4b4c-b770-1d96a8c4d9f8",
        "3534a987-933a-47bd-99e5-9664955394c1",
        "398f199d-2275-46a0-ad36-73d21d3b4eb0",
        "43ee887c-437f-4e4e-bb1c-95b03ee496f3",
        "4e25106a-be93-4f2b-9c7d-920e7c556689",
        "5c848311-56e2-4ca0-85b3-f350e17c1518",
        "5d36f00f-5758-4ce0-8ccb-db35f15ec3d3",
        "79648ba8-f009-4a75-8a84-1eb90f22d5c3",
        "8db20bd6-c75b-4f1d-a8f6-89343bbf0f34",
        "9bbac544-aab1-4804-97d8-86347d476db3",
        "a362dbfb-f0fb-40ba-a89b-4a39f9dead2e",
        "b6414a4c-61fa-429e-9a45-6eb97566ea4b",
        "bdd65244-ccbd-4c9c-b2a2-92b230008c2a",
        "c4bbc915-bd2f-4b67-a251-ea3c2076724c",
        "c674ec4f-35aa-45e8-8829-b7676b084cb9",
        "dd84367c-cc8c-4e36-bfcc-7c8522145b9e",
        "fec9a389-5c68-41c3-b0bd-00d390e2170b",
      ).distinct

      modulesToDelete.foreach { module =>
        repo.delete(UUID.fromString(module)).onComplete {
          case Success(_) => logger.info(s"deleted module $module")
          case Failure(e) => logger.error(s"failed to delete module $module", e)
        }
      }

      NoContent
    }
}
