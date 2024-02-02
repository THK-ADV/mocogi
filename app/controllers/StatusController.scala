package controllers

import models.core.{Status => ModuleStatus}
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.StatusService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class StatusController @Inject() (
    cc: ControllerComponents,
    val service: StatusService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[ModuleStatus] {
  override implicit val writes: Writes[ModuleStatus] = ModuleStatus.writes
}
