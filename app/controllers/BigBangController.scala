package controllers

import auth.AuthorizationAction
import catalog.PreviewMergeActor
import controllers.actions.{AdminCheck, PermissionCheck}
import models.Semester
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class BigBangController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    actor: PreviewMergeActor,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AdminCheck
    with PermissionCheck {

  def go() =
    auth andThen isAdmin apply { _ =>
      actor.createMergeRequest(Semester("wise_2024"))
      NoContent
    }
}
