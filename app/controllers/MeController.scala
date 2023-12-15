package controllers

import auth.AuthorizationAction
import controllers.actions.PersonAction
import controllers.formats.PersonFormat
import database.repo.{PersonRepository, StudyProgramPersonRepository}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class MeController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    studyProgramPersonRepository: StudyProgramPersonRepository,
    val personRepository: PersonRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with PersonAction
    with PersonFormat {

  def me() =
    auth andThen personAction async { r =>
      studyProgramPersonRepository
        .getDirectors(r.person.id)
        .map(xs =>
          Ok(
            Json.obj(
              "me" -> Json.toJson(r.person),
              "director" -> Json.toJson(xs)
            )
          )
        )
    }
}
