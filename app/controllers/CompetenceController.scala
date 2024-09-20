package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.ModuleCompetence
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.CompetenceService

@Singleton
final class CompetenceController @Inject() (
    cc: ControllerComponents,
    val service: CompetenceService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[ModuleCompetence] {
  implicit override val writes: Writes[ModuleCompetence] = ModuleCompetence.writes
}
