package controllers

import models.core.Location
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.LocationService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class LocationController @Inject() (
    cc: ControllerComponents,
    val service: LocationService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with SimpleYamlController[Location] {
  override implicit val writes: Writes[Location] = Location.writes
}
