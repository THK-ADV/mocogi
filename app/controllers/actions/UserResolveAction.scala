package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.*
import controllers.UsesClientErrors
import database.repo.PermissionRepository
import models.core.Identity
import models.EmploymentType.Unknown
import permission.PermissionType
import permission.Permissions
import play.api.mvc.ActionRefiner
import play.api.mvc.Result
import play.api.mvc.WrappedRequest

case class UserRequest[A](
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

private[controllers] trait UserResolveAction extends UsesClientErrors {
  implicit def ctx: ExecutionContext
  implicit def permissionRepository: PermissionRepository

  def resolveUser: ActionRefiner[TokenRequest, UserRequest] =
    new ActionRefiner[TokenRequest, UserRequest] {
      def executionContext: ExecutionContext = ctx

      protected override def refine[A](request: TokenRequest[A]): Future[Either[Result, UserRequest[A]]] =
        request.token match {
          case _: Token.UserToken    => getByUsername(request)
          case s: Token.ServiceToken => serviceTokenUser(request, s)
        }

      private def serviceTokenUser[A](request: TokenRequest[A], token: Token.ServiceToken) = {
        val person = Identity.Person("", "", "", "", Nil, "", None, isActive = true, Unknown, None)
        val perms  = permissionsFromKeycloakServiceRoles(token.roles)
        Future.successful(Right(UserRequest(person, perms, request)))
      }

      private def permissionsFromKeycloakServiceRoles(roles: Set[String]): Permissions =
        if roles.contains(PermissionType.Admin.label) then Permissions(Map(PermissionType.Admin -> Set.empty))
        else Permissions(Map.empty)

      private def getByUsername[A](request: TokenRequest[A]) =
        permissionRepository
          .all(request.campusId)
          .map {
            case Some((p, perms)) =>
              if p.isActive then Right(UserRequest(p, perms, request))
              else
                Left(
                  clientErrors.badRequest(
                    request,
                    s"user with campusId ${request.campusId.value} is inactive"
                  )
                )
            case None =>
              Left(
                clientErrors.badRequest(
                  request,
                  s"no user found for campusId ${request.campusId.value}"
                )
              )
          }
    }
}
