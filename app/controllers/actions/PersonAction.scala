package controllers.actions

import auth.UserTokenRequest
import controllers.actions.PersonAction.PersonRequest
import database.repo.PersonRepository
import models.CampusId
import models.core.Person
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ActionRefiner, Result, WrappedRequest}

import scala.concurrent.{ExecutionContext, Future}

trait PersonAction {
  implicit def ctx: ExecutionContext
  implicit def personRepository: PersonRepository

  private def campusIdProxy[A](r: UserTokenRequest[A]): CampusId =
    r.getQueryString("campusId").fold(r.campusId)(CampusId.apply)

  def personAction = new ActionRefiner[UserTokenRequest, PersonRequest] {
    def executionContext = ctx

    override protected def refine[A](
        request: UserTokenRequest[A]
    ): Future[Either[Result, PersonRequest[A]]] = {
      // TODO DEBUG ONLY
      val campusId = campusIdProxy(request)
      // TODO DEBUG ONLY
      val newRequest =
        request.copy(token = request.token.copy(username = campusId.value))
      personRepository
        .getByCampusId(campusId)
        .map {
          case Some(p) => Right(PersonRequest(p, newRequest))
          case None =>
            Left(
              BadRequest(
                Json.obj(
                  "message" -> s"no user found for campusId ${campusId.value}"
                )
              )
            )
        }
    }
  }
}

object PersonAction {
  case class PersonRequest[A](
      person: Person.Default,
      request: UserTokenRequest[A]
  ) extends WrappedRequest[A](request)
}
