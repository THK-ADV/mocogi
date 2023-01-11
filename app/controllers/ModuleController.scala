package controllers

import play.api.libs.json.{Format, Json}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{MetadataService, Module}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleController @Inject() (
    cc: ControllerComponents,
    service: MetadataService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {
  implicit val moduleFormat: Format[Module] =
    Json.format[Module]

  def all() =
    Action.async { request =>
      service
        .allModules(request.queryString)
        .map(xs => Ok(Json.toJson(xs)))
    }
}
