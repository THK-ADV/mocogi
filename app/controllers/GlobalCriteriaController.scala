package controllers

import models.core.ModuleGlobalCriteria
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.GlobalCriteriaService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class GlobalCriteriaController @Inject() (
    cc: ControllerComponents,
    val service: GlobalCriteriaService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[ModuleGlobalCriteria] {
  override implicit val writes: Writes[ModuleGlobalCriteria] = ModuleGlobalCriteria.writes
}
