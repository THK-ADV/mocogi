package controllers

import basedata.FocusArea
import controllers.json.FocusAreaFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class FocusAreaController @Inject() (
    cc: ControllerComponents,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with FocusAreaFormat
    with YamlController[FocusArea] {
  override implicit val writes: Writes[FocusArea] = focusAreaFormat
  override val service = ???
}
