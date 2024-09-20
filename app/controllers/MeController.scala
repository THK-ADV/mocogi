package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.PersonAction
import database.repo.core.IdentityRepository
import database.repo.core.StudyProgramPersonRepository
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents

@Singleton
final class MeController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    studyProgramPersonRepository: StudyProgramPersonRepository,
    val identityRepository: IdentityRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with PersonAction {

  def me() =
    auth.andThen(personAction).async { r =>
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
