package controllers.core

import controllers.formats.CompetencesFormat
import models.core.Competence
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.CompetenceService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class CompetenceController @Inject() (
    cc: ControllerComponents,
    val service: CompetenceService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with CompetencesFormat
    with SimpleYamlController[Competence] {
  override implicit val writes: Writes[Competence] = competenceFormat
}