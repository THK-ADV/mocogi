package controllers

import controllers.formats.ModuleCompendiumOutputFormat
import ops.FileOps
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import providers.ConfigReader
import service.ModuleCompendiumService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleCompendiumController @Inject() (
    cc: ControllerComponents,
    service: ModuleCompendiumService,
    configReader: ConfigReader,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleCompendiumOutputFormat {

  def all() =
    Action.async { request =>
      service
        .all(request.queryString)
        .map(xs => Ok(Json.toJson(xs)))
    }

  def get(id: UUID) =
    Action.async { _ =>
      service.get(id).map(x => Ok(Json.toJson(x)))
    }

  def getFile(id: UUID) =
    Action { _ =>
      val folder = configReader.outputFolderPath
      val filename = s"$id.html"
      Ok.sendFile(
        content = FileOps.getFile(s"$folder/$filename").get,
        fileName = _ => Some(filename)
      )
    }
}
