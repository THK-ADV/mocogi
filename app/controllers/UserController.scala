package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.PermissionRepository
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import service.StudyProgramPrivilegesService

@Singleton
final class UserController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    studyProgramPrivilegesService: StudyProgramPrivilegesService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserResolveAction {

  def userInfo() =
    auth.andThen(resolveUser).async { (r: UserRequest[AnyContent]) =>
      permissionRepository.getUserInfo(r.person.id, r.request.campusId, r.permissions).map { js =>
        Ok(Json.toJsObject(js).+(("person", Json.toJson(r.person))))
      }
    }

  def studyProgramPrivileges() =
    auth.andThen(resolveUser).async { (r: UserRequest[AnyContent]) =>
      studyProgramPrivilegesService
        .getStudyProgramPrivileges(r.person.id, r.permissions)
        .map(xs => Ok(Json.toJson(xs)))
    }
}
