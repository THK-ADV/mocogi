package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.history.ModuleHistoryService
import controllers.actions.UserResolveAction
import auth.AuthorizationAction
import security.ClientErrorResponse
import database.repo.PermissionRepository
import permission.ModuleHistoryCheck
import service.ModuleUpdatePermissionService

@Singleton
final class ModuleHistoryController @Inject() (
    cc: ControllerComponents,
    private val moduleHistoryService: ModuleHistoryService,
    val auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleHistoryCheck
    with UserResolveAction {

  def getModuleHistory(module: UUID) =
    auth.andThen(resolveUser).andThen(canViewModuleHistory(module)).async { _ =>
      moduleHistoryService
        .getModuleHistory(module)
        .map(versions => Ok(Json.toJson(versions)))
    }
}
