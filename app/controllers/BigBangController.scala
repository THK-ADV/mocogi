package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import auth.Role.Admin
import catalog.ModuleCatalogService
import catalog.PreviewMergeActor
import controllers.actions.RoleCheck
import models.Semester
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request

@deprecated("Discuss the existence of this class")
@Singleton
final class BigBangController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    actor: PreviewMergeActor,
    moduleCatalogService: ModuleCatalogService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with RoleCheck {

  private def semester = "wise_2024"

  def go() =
    auth.andThen(hasRole(Admin)).apply { (_: Request[AnyContent]) =>
      actor.createMergeRequest(Semester(semester))
      NoContent
    }

  def goCatalogs() =
    auth.andThen(hasRole(Admin)).async { _ =>
      moduleCatalogService
        .createAndOpenMergeRequest(semester)
        .map(_ => NoContent)
    }
}
