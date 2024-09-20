package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.UserTokenRequest
import play.api.mvc.ActionFilter
import play.api.mvc.Result

trait AdminCheck { self: PermissionCheck =>

  def isAdmin = new ActionFilter[UserTokenRequest] {
    protected override def filter[A](
        request: UserTokenRequest[A]
    ): Future[Option[Result]] =
      if (request.campusId.value == "adobryni")
        Future.successful(None) // TODO DEBUG ONLY
      else
        toResult(
          Future.successful(false),
          request
        )

    protected override def executionContext: ExecutionContext = ctx
  }
}
