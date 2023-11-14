package controllers.actions

import controllers.actions.PersonAction.PersonRequest
import play.api.mvc.{ActionFilter, Result}
import service.ModuleApprovalService

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait ApprovalCheck { self: PermissionCheck =>
  protected def approvalService: ModuleApprovalService

  def hasPermissionToApproveReview(id: UUID) =
    new ActionFilter[PersonRequest] {
      override protected def filter[A](
          request: PersonRequest[A]
      ): Future[Option[Result]] =
        toResult(
          approvalService.hasPendingApproval(id, request.person),
          request.request
        )

      override protected def executionContext: ExecutionContext = ctx
    }
}
