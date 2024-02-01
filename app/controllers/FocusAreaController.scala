package controllers

import models.core.FocusArea
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.FocusAreaService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class FocusAreaController @Inject() (
    cc: ControllerComponents,
    val service: FocusAreaService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with SimpleYamlController[FocusArea] {
  override implicit val writes: Writes[FocusArea] = FocusArea.writes
}
