package controllers

import controllers.formats.ModuleCompendiumOutputFormat
import ops.FileOps
import play.api.libs.json.Json
import play.api.mvc.{
  AbstractController,
  AnyContent,
  ControllerComponents,
  Request
}
import providers.ConfigReader
import service.ModuleCompendiumService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

object ModuleCompendiumController {
  lazy val languageAttribute = "lang"
}

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
    Action { implicit r =>
      val lang = parseLang
      val folder = configReader.outputFolderPath
      val filename = s"$id.html"
      val path = s"$folder/$lang/$filename"
      val file = FileOps.getFile(path).get
      Ok.sendFile(content = file, fileName = _ => Some(filename))
    }

  private def parseLang(implicit r: Request[AnyContent]): String =
    r.getQueryString(ModuleCompendiumController.languageAttribute)
      .filter(s => s == "de" || s == "en")
      .getOrElse("de")
}
