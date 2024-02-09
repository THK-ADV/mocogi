package controllers.actions

import auth.UserTokenRequest
import play.api.mvc.{ActionFilter, Result}

import scala.concurrent.{ExecutionContext, Future}

trait AdminCheck { self: PermissionCheck =>

  def isAdmin = new ActionFilter[UserTokenRequest] {
    override protected def filter[A](
        request: UserTokenRequest[A]
    ): Future[Option[Result]] =
      if (request.campusId.value == "adobryni")
        Future.successful(None) // TODO DEBUG ONLY
      else
        toResult(
          Future.successful(false),
          request
        )

    override protected def executionContext: ExecutionContext = ctx
  }
}
