package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.ModuleGlobalCriteria
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.GlobalCriteriaService

@Singleton
final class GlobalCriteriaController @Inject() (
    cc: ControllerComponents,
    val service: GlobalCriteriaService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[ModuleGlobalCriteria] {
  implicit override val writes: Writes[ModuleGlobalCriteria] = ModuleGlobalCriteria.writes
}
