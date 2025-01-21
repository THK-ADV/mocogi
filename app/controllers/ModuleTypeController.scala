package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.ModuleType
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.ModuleTypeService

@Singleton
final class ModuleTypeController @Inject() (
    cc: ControllerComponents,
    val service: ModuleTypeService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[ModuleType] {
  implicit override val writes: Writes[ModuleType] = ModuleType.writes
}
