package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.ModuleLocation
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.LocationService

@Singleton
final class LocationController @Inject() (
    cc: ControllerComponents,
    val service: LocationService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[ModuleLocation] {
  implicit override val writes: Writes[ModuleLocation] = ModuleLocation.writes
}
