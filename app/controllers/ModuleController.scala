package controllers

import controllers.formats.MetadataOutputFormat
import play.api.libs.json.{Format, Json}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{Module, ModuleCompendiumService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleController @Inject() (
    cc: ControllerComponents,
    service: ModuleCompendiumService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with MetadataOutputFormat {
  implicit val moduleFormat: Format[Module] =
    Json.format[Module]

  def allModules() =
    Action.async { request =>
      service
        .allModules(request.queryString)
        .map(xs => Ok(Json.toJson(xs)))
    }

  def allModuleMetadata() =
    Action.async { request =>
      service
        .allMetadata(request.queryString)
        .map(xs => Ok(Json.toJson(xs)))
    }
}
