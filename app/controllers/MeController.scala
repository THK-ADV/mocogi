package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import controllers.actions.PersonAction
import database.repo.core.IdentityRepository
import database.repo.core.StudyProgramPersonRepository
import database.repo.PermissionRepository
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents

@Singleton
final class MeController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    identityRepository: IdentityRepository,
    studyProgramPersonRepository: StudyProgramPersonRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with PersonAction {

  def me() =
    auth.andThen(personAction).async { r =>
      if r.isNewApi then
        r.person.campusId match {
          case Some(cid) =>
            identityRepository.getUserInfo(r.person.id, cid).map { js =>
              Ok(Json.parse(js).validate[JsObject].get.+(("person", Json.toJson(r.person))))
            }
          case None => Future.successful(NotFound)
        }
      else
        studyProgramPersonRepository
          .getStudyProgramPrivileges(r.person.id)
          .map(xs =>
            Ok(
              Json.obj(
                "me"         -> Json.toJson(r.person),
                "privileges" -> Json.toJson(xs)
              )
            )
          )
    }
}
