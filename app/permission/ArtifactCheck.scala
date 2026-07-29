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
    canAccessArtifact(po, _.artifactsCreatePermissions, s"to create artifacts for $po")

  /**
   * This method checks if the user can preview artifacts for the given PO.
   */
  def canPreviewArtifact(po: String) =
    canAccessArtifact(po, _.artifactsPreviewPermissions, s"to preview artifacts for $po")

  private def canAccessArtifact(po: String, permissions: Permissions => Set[String], reason: => String) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] =
        Future.successful(
          Option.unless(request.permissions.isAdmin || permissions(request.permissions).contains(po))(
            forbiddenForUser(request, request.request.token.username, Some(reason))
          )
        )

      protected override def executionContext: ExecutionContext = ctx
    }
}
