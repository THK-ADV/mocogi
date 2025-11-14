package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.core.StudyProgramPersonRepository
import database.repo.PermissionRepository
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import service.core.IdentityService

@Singleton
final class UserController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    identityService: IdentityService,
    studyProgramPersonRepository: StudyProgramPersonRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserResolveAction {

  def userInfo() =
    auth.andThen(resolveUser).async { (r: UserRequest[AnyContent]) =>
      identityService.getUserInfo(r.person.id, r.request.campusId, r.permissions).map { js =>
        Ok(Json.toJsObject(js).+(("person", Json.toJson(r.person))))
      }
    }
}

//studyProgramPersonRepository
//  .getStudyProgramPrivileges(r.person.id)
//  .map(xs =>
//    Ok(
//      Json.obj(
//        "me" -> Json.toJson(r.person),
//        "privileges" -> Json.toJson(xs)
//      )
//    )
//  )
