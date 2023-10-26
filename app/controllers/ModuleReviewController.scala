package controllers

import auth.AuthorizationAction
import models.ModuleReviewStatus
import play.api.libs.json.Format
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

  // TODO
}
