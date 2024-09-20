package controllers.actions

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.PersonAction.PersonRequest
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import service.ModuleApprovalService

trait ApprovalCheck { self: PermissionCheck =>
  protected def approvalService: ModuleApprovalService

  def hasPermissionToApproveReview(id: UUID) =
    new ActionFilter[PersonRequest] {
      protected override def filter[A](
          request: PersonRequest[A]
      ): Future[Option[Result]] =
        toResult(
          approvalService.hasPendingApproval(id, request.person),
          request.request
        )

      protected override def executionContext: ExecutionContext = ctx
    }
}
