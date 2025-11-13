package controllers.actions

import ops.||
import play.api.libs.json.Json
import play.api.mvc.{ActionFilter, Result}
import play.api.mvc.Results.Forbidden
import service.ModuleUpdatePermissionService

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait ModuleDraftCheck {
  implicit def moduleUpdatePermissionService: ModuleUpdatePermissionService
  implicit def ctx: ExecutionContext

  @deprecated
  def moduleInReaccreditation[A](moduleId: UUID, request: UserRequest[A]): Future[Boolean] = ???

  /**
   * This method checks if the user is allowed to edit the module. The verification process is three-stage:
   * 1. Checks if the user is directly authorized (inherited or granted permission)
   * 2. Checks if the user is the author (created the module)
   * 3. Checks if the user is authorized through a role like admin or PAV
   */
  def canEditModule(module: UUID) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        val hasPermission = moduleUpdatePermissionService.hasPermissionFor(module, request.request.campusId).andThen { case Success(true) => println(s"[${request.request.toString}] hasPermissionFor")} ||
          moduleUpdatePermissionService.isAuthorOf(module, request.person.id).andThen { case Success(true) => println(s"[${request.request.toString}] isAuthorOf")} ||
          request.permissions.modulePermissions
            .map(pos => moduleUpdatePermissionService.isModulePartOfPO(module, pos).andThen { case Success(true) => println(s"[${request.request.toString}] isModulePartOfPO")})
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
