package controllers

import models.core.StudyFormType
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.StudyFormTypeService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class StudyFormTypeController @Inject() (
    cc: ControllerComponents,
    val service: StudyFormTypeService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with SimpleYamlController[StudyFormType] {
  override implicit val writes: Writes[StudyFormType] = StudyFormType.writes
}
