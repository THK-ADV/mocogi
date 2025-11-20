package permission

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import ops.||
import play.api.libs.json.Json
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.mvc.Results.Forbidden
import service.ModuleUpdatePermissionService

trait ModuleDraftCheck {
  protected def moduleUpdatePermissionService: ModuleUpdatePermissionService
  protected implicit def ctx: ExecutionContext

  /**
   * This method checks if the user is allowed to edit the module. The verification process is three-stage:
   * 1. Checks if the user is directly authorized (inherited or granted permission)
   * 2. Checks if the user is the author (created the module)
   * 3. Checks if the user is authorized through a role such as admin or PAV
   */
  def canEditModule(module: UUID) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        val hasPermission = moduleUpdatePermissionService.hasPermissionFor(module, request.request.campusId) ||
          moduleUpdatePermissionService.isAuthorOf(module, request.person.id) ||
          request.permissions.modulePermissions
            .map(pos => moduleUpdatePermissionService.isModulePartOfPO(module, pos))
            .getOrElse(Future.successful(false))

        hasPermission.map {
          case true => None
          case false =>
            Some(
              Forbidden(
                Json.obj(
                  "request" -> request.toString(),
                  "message" -> s"user ${request.request.token.username} has insufficient permissions to edit the module"
                )
              )
            )
        }
      }

      protected override def executionContext: ExecutionContext = ctx
    }
}
