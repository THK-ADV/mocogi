package controllers

import basedata.ModuleType
import controllers.json.ModuleTypeFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ModuleTypeService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleTypeController @Inject() (
    cc: ControllerComponents,
    val service: ModuleTypeService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleTypeFormat
    with YamlController[ModuleType, ModuleType] {
  override implicit val writesOut: Writes[ModuleType] = moduleTypeFormat
  override implicit val writesIn: Writes[ModuleType] = moduleTypeFormat
}
