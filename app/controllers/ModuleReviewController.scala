package controllers

import auth.AuthorizationAction
import models.{ModuleReviewStatus, User}
import play.api.libs.json.{Format, JsArray, Json}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ModuleReviewService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleReviewController @Inject() (
    cc: ControllerComponents,
    service: ModuleReviewService,
    auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  implicit val mrsFmt: Format[ModuleReviewStatus] =
    Format.of[String].bimap(ModuleReviewStatus.apply, _.id)

  def getAllForUser() =
    auth async { r =>
      service
        .getForUser(User(r.token.username))
        .map(xs =>
          Ok(JsArray(xs.map { case (module, status, approved) =>
            Json.obj(
              "module" -> module,
              "status" -> status,
              "approved" -> approved
            )
          }))
        )
    }
}
