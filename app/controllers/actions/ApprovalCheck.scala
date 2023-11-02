package controllers.actions

import auth.UserTokenRequest
import models.User
import play.api.mvc.{ActionFilter, Result}
import service.ModuleApprovalService

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait ApprovalCheck { self: PermissionCheck =>
  protected def approvalService: ModuleApprovalService

  def hasPermissionToApproveReview(id: UUID) =
    new ActionFilter[UserTokenRequest] {
      override protected def filter[A](
          request: UserTokenRequest[A]
      ): Future[Option[Result]] = {
        // TODO DEBUG ONLY
        val username =
          request.getQueryString("user").getOrElse(request.token.username)
        val user = User(username)
        toResult(approvalService.hasPendingApproval(id, user), request)
      }

      override protected def executionContext: ExecutionContext = ctx
    }
}
