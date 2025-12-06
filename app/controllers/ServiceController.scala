package controllers

import javax.inject.Inject
import javax.inject.Named

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import auth.TokenRequest
import org.apache.pekko.actor.ActorRef
import permission.Role
import permission.ServiceAccountCheck
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import service.notification.ReviewNotificationActor.DryRun
import service.notification.ReviewNotificationActor.NotifyAllPendingReviews

final class ServiceController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    @Named("ReviewNotificationActor") actor: ActorRef,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ServiceAccountCheck {

  def notifyReviewer() =
    auth.andThen(hasRole(Role.NotifyReviewer)).apply { (r: TokenRequest[AnyContent]) =>
      val dryRun  = r.getQueryString("dryRun").flatMap(_.toBooleanOption).getOrElse(true)
      val message = if dryRun then DryRun else NotifyAllPendingReviews
      actor ! message
      NoContent
    }
}
