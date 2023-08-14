package controllers

import auth.{AuthorizationAction, ModuleUpdatePermissionCheck}
import git.api.GitBranchService
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ModuleUpdatePermissionService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class GitController @Inject() (
    cc: ControllerComponents,
    val service: GitBranchService,
    val auth: AuthorizationAction,
    implicit val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleUpdatePermissionCheck {

  def createBranch(moduleId: UUID) =
    auth andThen hasPermissionForModule(moduleId) async { _ =>
      service
        .createBranch(moduleId)
        .map(a => Created(Json.toJson(a)))
    }

  def branchForUser() =
    auth async { r =>
      service.branchForUser(r.token.username).map(a => Ok(Json.toJson(a)))
    }

  def deleteBranch(moduleId: UUID) =
    auth andThen hasPermissionForModule(moduleId) async { _ =>
      service.deleteBranch(moduleId).map(_ => NoContent)
    }
}
