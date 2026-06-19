package permission

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import controllers.UsesClientErrors
import ops.||
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import service.ModuleUpdatePermissionService

trait ModuleHistoryCheck extends UsesClientErrors {
  protected def moduleUpdatePermissionService: ModuleUpdatePermissionService
  protected implicit def ctx: ExecutionContext

  /**
   * This method checks if the user is allowed to view the module history. The verification process is three-stage:
   * 1. Checks if the user is directly authorized (inherited or granted permission)
   * 2. Checks if the user is the author (created the module)
   * 3. Checks if the user is authorized through a role such as admin or PAV
   */
  def canViewModuleHistory(module: UUID) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        val hasPermission = moduleUpdatePermissionService.hasPermissionFor(module, request.request.campusId) ||
          moduleUpdatePermissionService.isAuthorOf(module, request.person.id) ||
          request.permissions.modulePermissions
            .filter(_.nonEmpty)
            .map(pos => moduleUpdatePermissionService.isModulePartOfPO(module, pos))
            .getOrElse(Future.successful(false))

        hasPermission.map {
          case true  => None
          case false =>
            Some(
              forbiddenForUser(request, request.request.token.username, Some("to view the module history"))
            )
        }
      }

      protected override def executionContext: ExecutionContext = ctx
    }
}
