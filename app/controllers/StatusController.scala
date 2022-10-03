package controllers

import basedata.{Status => ModuleStatus}
import controllers.json.StatusFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.StatusService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class StatusController @Inject() (
    cc: ControllerComponents,
    val service: StatusService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with StatusFormat
    with YamlController[ModuleStatus] {
  override implicit val writes: Writes[ModuleStatus] = statusFormat
}
