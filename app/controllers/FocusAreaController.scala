package controllers

import basedata.FocusArea
import controllers.json.FocusAreaFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.YamlService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class FocusAreaController @Inject() (
    cc: ControllerComponents,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with FocusAreaFormat
    with YamlController[FocusArea, FocusArea] {
  override val service = new YamlService[FocusArea, FocusArea] {
    override def repo = ???

    override def parser = ???

    override def toInput(output: FocusArea) = ???
  }
  override implicit val writesOut: Writes[FocusArea] = focusAreaFormat
  override implicit val writesIn: Writes[FocusArea] = focusAreaFormat
}
