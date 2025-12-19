package permission

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import controllers.ModuleReviewRequest
import ops.||
import permission.PermissionType.ApprovalFastForward
import play.api.libs.json.Json
import play.api.mvc.ActionFilter
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.mvc.Results.Forbidden
import service.ModuleReviewService
import service.ModuleUpdatePermissionService

trait ModuleReviewCheck {
  protected def moduleReviewService: ModuleReviewService
  protected def moduleUpdatePermissionService: ModuleUpdatePermissionService
  protected implicit def ctx: ExecutionContext

  /**
   * This method checks if the user is allowed to preview a module. The verification process is two-stage:
   * 1. Checks if the user is directly authorized (e.g., is a PAV)
   * 2. Checks if the user has admin permissions
   */
  def canReviewModule =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        if request.permissions.isAdmin then Future.successful(None)
        else {
          request.body match {
            case moduleReviewRequest: ModuleReviewRequest =>
              moduleReviewService.hasPendingReview(moduleReviewRequest.reviews, request.person.id).map {
                case true  => None
                case false =>
                  Some(
                    Forbidden(
                      Json.obj(
                        "request" -> request.toString(),
                        "message" -> s"user ${request.request.token.username} has insufficient permissions to review the module"
                      )
                    )
                  )
              }
            case _ =>
              Future.successful(Some(Results.BadRequest(Json.obj("message" -> "Invalid request body"))))
          }
        }
      }

      protected override def executionContext: ExecutionContext = ctx
    }

  /**
   * This method checks if the user is allowed to fast-forward a review. The verification process is two-stage:
   * 1. Checks if the user has admin permissions
   * 2. Checks if the user has the permission for modules in the PO
   */
  def canFastForwardReview(moduleId: UUID) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        val hasPermission = Future.successful(request.permissions.isAdmin) || {
          request.permissions.get(ApprovalFastForward) match {
            case Some(pos) if pos.nonEmpty => moduleUpdatePermissionService.isModulePartOfPO(moduleId, pos)
            case _                         => Future.successful(false)
          }
        }
        hasPermission.map {
          case true  => None
          case false =>
            Some(
              Forbidden(
                Json.obj(
                  "request" -> request.toString(),
                  "message" -> s"user ${request.request.token.username} has insufficient permissions to fast-forward review for the module"
                )
              )
            )
        }
      }

      protected override def executionContext: ExecutionContext = ctx
    }
}
