package controllers

import basedata.PO
import controllers.json.POFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.POService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class POController @Inject() (
    cc: ControllerComponents,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[PO]
    with POFormat {
  override implicit val writes: Writes[PO] = poFormat
  override val service = POService
}
