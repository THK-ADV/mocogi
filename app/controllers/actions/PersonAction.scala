package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.CampusId
import auth.UserTokenRequest
import controllers.actions.PersonAction.PersonRequest
import database.repo.core.IdentityRepository
import models.core.Identity
import play.api.libs.json.Json
import play.api.mvc.ActionRefiner
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import play.api.mvc.WrappedRequest

trait PersonAction {
  implicit def ctx: ExecutionContext
  implicit def identityRepository: IdentityRepository

  private def campusIdProxy[A](r: UserTokenRequest[A]): CampusId =
    r.getQueryString("campusId").fold(r.campusId)(CampusId.apply)

  def personAction = new ActionRefiner[UserTokenRequest, PersonRequest] {
    def executionContext = ctx

    protected override def refine[A](
        request: UserTokenRequest[A]
    ): Future[Either[Result, PersonRequest[A]]] = {
      // TODO DEBUG ONLY
      val campusId = campusIdProxy(request)
      // TODO DEBUG ONLY
      val newRequest =
        request.copy(token = request.token.copy(username = campusId.value))
      identityRepository
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
      person: Identity.Person,
      request: UserTokenRequest[A]
  ) extends WrappedRequest[A](request)
}
