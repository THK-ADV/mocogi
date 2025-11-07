package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.*
import database.repo.PermissionRepository
import models.core.Identity
import models.EmploymentType.Unknown
import play.api.libs.json.Json
import play.api.mvc.ActionRefiner
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import play.api.mvc.WrappedRequest

case class PersonRequest[A](
    person: Identity.Person,
    permissions: Permissions,
    request: TokenRequest[A]
) extends WrappedRequest[A](request) {

  def accreditationPOs: Option[List[String]] = {
    def parse(role: String): Option[List[String]] = {
      if (!role.startsWith("[") || !role.endsWith("]")) return None

      val content = role.drop(1).dropRight(1)
      if (content.isEmpty) return None

      val pos = content.split(",").map(_.trim).filter(_.nonEmpty).toList
      Option.when(pos.nonEmpty)(pos)
    }

    val prefix = "accreditation-member_"
    request.token.roles.find(_.startsWith(prefix)).flatMap(a => parse(a.drop(prefix.length)))
  }
}

trait PersonAction {
  implicit def ctx: ExecutionContext
  implicit def permissionRepository: PermissionRepository

  def personAction: ActionRefiner[TokenRequest, PersonRequest] =
    new ActionRefiner[TokenRequest, PersonRequest] {
      def executionContext: ExecutionContext = ctx

      protected override def refine[A](request: TokenRequest[A]): Future[Either[Result, PersonRequest[A]]] =
        request.token match {
          case _: Token.UserToken    => getByUsername(request)
          case _: Token.ServiceToken => adminUser(request)
        }

      private def adminUser[A](request: TokenRequest[A]) = {
        val adminUser = Identity.Person("", "", "", "", Nil, "", None, isActive = true, Unknown, None)
        val adminPerm = Permissions(Map((PermissionType.Admin, Nil)))
        Future.successful(Right(PersonRequest(adminUser, adminPerm, request)))
      }

      private def getByUsername[A](request: TokenRequest[A]) =
        permissionRepository
          .all(request.campusId)
          .map {
            case Some((p, perms)) =>
              if p.isActive then Right(PersonRequest(p, perms, request))
              else Left(BadRequest(Json.obj("message" -> s"user with campusId ${request.campusId.value} is inactive")))
            case None =>
              Left(BadRequest(Json.obj("message" -> s"no user found for campusId ${request.campusId.value}")))
          }
    }
}
