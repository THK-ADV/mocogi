package controllers

import basedata.StudyFormType
import controllers.json.StudyFormTypeFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.StudyFormTypeService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class StudyFormTypeController @Inject() (
    cc: ControllerComponents,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[StudyFormType]
    with StudyFormTypeFormat {
  override implicit val writes: Writes[StudyFormType] = studyFormTypeFormat
  override val service = StudyFormTypeService
}