package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import catalog.ModuleCatalogService
import catalog.PreviewMergeActor
import catalog.Semester
import controllers.actions.AdminCheck
import controllers.actions.PermissionCheck
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request

@Singleton
final class BigBangController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    actor: PreviewMergeActor,
    moduleCatalogService: ModuleCatalogService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AdminCheck
    with PermissionCheck {

  private def semester = "wise_2024"

  def go() =
    auth.andThen(isAdmin).apply { (_: Request[AnyContent]) =>
      actor.createMergeRequest(Semester(semester))
      NoContent
    }

  def goCatalogs() =
    auth.andThen(isAdmin).async { _ =>
      moduleCatalogService
        .createAndOpenMergeRequest(semester)
        .map(_ => NoContent)
    }
}
