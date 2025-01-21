package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.Identity
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.IdentityService

@Singleton
final class IdentityController @Inject() (
    cc: ControllerComponents,
    override val service: IdentityService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[Identity] {
  implicit override val writes: Writes[Identity] = Identity.writes
}
