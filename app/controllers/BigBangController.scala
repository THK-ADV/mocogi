package controllers

import auth.AuthorizationAction
import catalog.{ModuleCatalogService, PreviewMergeActor}
import controllers.actions.{AdminCheck, PermissionCheck}
import models.{
  MergeRequestId,
  MergeRequestStatus,
  ModuleCatalogGenerationRequest,
  Semester
}
import play.api.mvc.{AbstractController, ControllerComponents}

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
    auth andThen isAdmin apply { _ =>
      actor.createMergeRequest(Semester(semester))
      NoContent
    }

  def goCatalogs() =
    auth andThen isAdmin async { _ =>
      moduleCatalogService
        .createAndOpenMergeRequest(
          ModuleCatalogGenerationRequest(
            MergeRequestId(1),
            semester,
            MergeRequestStatus.Open
          )
        )
        .map(_ => NoContent)
    }
}
