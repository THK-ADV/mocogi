package controllers

import auth.AuthorizationAction
import catalog.{ModuleCatalogService, PreviewMergeActor, Semester}
import controllers.actions.{AdminCheck, PermissionCheck}
import play.api.mvc.{
  AbstractController,
  AnyContent,
  ControllerComponents,
  Request
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

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
    auth andThen isAdmin apply { (_: Request[AnyContent]) =>
      actor.createMergeRequest(Semester(semester))
      NoContent
    }

  def goCatalogs() =
    auth andThen isAdmin async { _ =>
      moduleCatalogService
        .createAndOpenMergeRequest(semester)
        .map(_ => NoContent)
    }
}
