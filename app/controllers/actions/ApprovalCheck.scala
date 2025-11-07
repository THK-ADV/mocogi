package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.ModuleReviewRequest
import play.api.libs.json.Json
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import play.api.mvc.Results
import service.ModuleApprovalService

trait ApprovalCheck { self: PermissionCheck =>
  protected def approvalService: ModuleApprovalService

  def hasPermissionToApproveReview =
    new ActionFilter[PersonRequest] {
      protected override def filter[A](request: PersonRequest[A]): Future[Option[Result]] = {
        request.body match {
          case moduleReviewRequest: ModuleReviewRequest =>
            continueAsAdmin(
              request.request,
              otherwise = approvalService.hasPendingApprovals(moduleReviewRequest.reviews, request.person)
            )
          case _ =>
            Future.successful(Some(Results.BadRequest(Json.obj("message" -> "Invalid request body"))))
        }
      }

      protected override def executionContext: ExecutionContext = ctx
    }
}
