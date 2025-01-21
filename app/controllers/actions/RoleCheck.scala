package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.Role
import auth.TokenRequest
import play.api.mvc.ActionFilter
import play.api.mvc.Result

trait RoleCheck { self: PermissionCheck =>

  def hasRole(role: Role) =
    new ActionFilter[TokenRequest] {
      protected override def filter[A](request: TokenRequest[A]): Future[Option[Result]] =
        toResult(Future.successful(request.token.hasRole(role)), request)

      protected override def executionContext: ExecutionContext = ctx
    }
}
