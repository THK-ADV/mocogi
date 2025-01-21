package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.{ ModuleStatus => ModuleStatus }
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.StatusService

@Singleton
final class StatusController @Inject() (
    cc: ControllerComponents,
    val service: StatusService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[ModuleStatus] {
  implicit override val writes: Writes[ModuleStatus] = ModuleStatus.writes
}
