package controllers

import models.core.Identity
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.IdentityService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class IdentityController @Inject() (
    cc: ControllerComponents,
    override val service: IdentityService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[Identity] {
  override implicit val writes: Writes[Identity] = Identity.writes
}
