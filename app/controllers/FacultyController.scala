package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.Faculty
import play.api.cache.Cached
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.FacultyService

@Singleton
final class FacultyController @Inject() (
    cc: ControllerComponents,
    val service: FacultyService,
    val cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[Faculty] {
  implicit override val writes: Writes[Faculty] = Faculty.writes
}
