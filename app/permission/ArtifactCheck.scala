package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import controllers.actions.UserRequest
import controllers.UsesClientErrors
import play.api.mvc.ActionFilter
import play.api.mvc.Result

trait ArtifactCheck extends UsesClientErrors {
  protected implicit def ctx: ExecutionContext

  /**
   * This method checks if the user can create artifacts for the given PO.
   */
  def canCreateArtifact(po: String) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        Future.successful(
          Option.unless(request.permissions.isAdmin || request.permissions.artifactsCreatePermissions.contains(po))(
            forbiddenForUser(
              request,
              request.request.token.username,
              Some(s"to create artifacts for $po")
            )
          )
        )
      }

      protected override def executionContext: ExecutionContext = ctx
    }

  /**
   * This method checks if the user can preview artifacts for the given PO.
   */
  def canPreviewArtifact(po: String) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        Future.successful(
          Option.unless(request.permissions.isAdmin || request.permissions.artifactsPreviewPermissions.contains(po))(
            forbiddenForUser(
              request,
              request.request.token.username,
              Some(s"to preview artifacts for $po")
            )
          )
        )
      }

      protected override def executionContext: ExecutionContext = ctx
    }
}
