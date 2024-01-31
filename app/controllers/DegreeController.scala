package controllers

import models.core.Degree
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.DegreeService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class DegreeController @Inject() (
    cc: ControllerComponents,
    val service: DegreeService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with SimpleYamlController[Degree] {
  override implicit val writes: Writes[Degree] = Degree.writes
}
