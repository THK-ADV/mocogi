package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import controllers.ModuleReviewRequest
import database.repo.ModuleReviewRepository
import play.api.libs.json.Json
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import play.api.mvc.Results

trait ApprovalCheck { self: PermissionCheck =>
  protected def reviewRepository: ModuleReviewRepository

  def hasPermissionToApproveReview =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        request.body match {
          case moduleReviewRequest: ModuleReviewRequest =>
            continueAsAdmin(
              request.request,
              otherwise = Future.successful(false) // reviewRepository.hasPendingReview(moduleReviewRequest.reviews, request.person) // TODO
            )
          case _ =>
            Future.successful(Some(Results.BadRequest(Json.obj("message" -> "Invalid request body"))))
        }
      }

      protected override def executionContext: ExecutionContext = ctx
    }
}
