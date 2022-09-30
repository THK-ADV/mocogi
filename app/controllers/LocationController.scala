package controllers

import basedata.Location
import controllers.json.LocationFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.LocationService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class LocationController @Inject() (
    cc: ControllerComponents,
    val service: LocationService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with LocationFormat
    with YamlController[Location] {
  override implicit val writes: Writes[Location] = locationFormat
}
