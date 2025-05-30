package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.Token
import auth.TokenRequest
import controllers.actions.PersonAction.PersonRequest
import database.repo.core.IdentityRepository
import models.core.Identity
import models.EmploymentType.Unknown
import play.api.libs.json.Json
import play.api.mvc.ActionRefiner
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import play.api.mvc.WrappedRequest

trait PersonAction {
  implicit def ctx: ExecutionContext
  implicit def identityRepository: IdentityRepository

  def personAction = new ActionRefiner[TokenRequest, PersonRequest] {
    def executionContext = ctx

    protected override def refine[A](request: TokenRequest[A]): Future[Either[Result, PersonRequest[A]]] =
      request.token match {
        case _: Token.UserToken    => getByUsername(request)
        case _: Token.ServiceToken => adminUser(request)
      }

    private def adminUser[A](request: TokenRequest[A]) = {
      val admin = Identity.Person("", "", "", "", Nil, "", None, isActive = true, Unknown, None, None)
      Future.successful(Right(PersonRequest(admin, request)))
    }

    private def getByUsername[A](request: TokenRequest[A]) =
      identityRepository
        .getByCampusId(request.campusId)
        .map {
          case Some(p) =>
            if p.isActive then Right(PersonRequest(p, request))
            else Left(BadRequest(Json.obj("message" -> s"user with campusId ${request.campusId.value} is inactive")))
          case None => Left(BadRequest(Json.obj("message" -> s"no user found for campusId ${request.campusId.value}")))
        }
  }
}

object PersonAction {
  case class PersonRequest[A](
      person: Identity.Person,
      request: TokenRequest[A]
  ) extends WrappedRequest[A](request)
}
