package controllers

import controllers.formats.ModuleCompendiumOutputFormat
import ops.FileOps
import ops.FutureOps.OptionOps
import play.api.libs.json.Json
import play.api.mvc.{
  AbstractController,
  AnyContent,
  ControllerComponents,
  Request
}
import printing.PrintingLanguage
import providers.ConfigReader
import service.{ModuleCompendiumService, ModuleDraftService}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

object ModuleCompendiumController {
  private lazy val languageAttribute = "lang"
}

@Singleton
final class ModuleCompendiumController @Inject() (
    cc: ControllerComponents,
    service: ModuleCompendiumService,
    draftService: ModuleDraftService,
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

  def getLatest(id: UUID) = // TODO only which can edit or which should review
    Action.async { _ =>
      draftService
        .getByModuleOpt(id)
        .map(_.map(_.data))
        .orElse(service.getFromStaging(id).map(mc => Json.toJson(mc)))
        .map(j => Ok(j))
    }

  def getStaging(id: UUID) =
    Action.async { _ =>
      service.getFromStaging(id).map(mc => Ok(Json.toJson(mc)))
    }

  def getFile(id: UUID) =
    Action { implicit r =>
      val lang = parseLang
      val folder = configReader.outputFolderPath
      val filename = s"$id.html"
      val path = s"$folder/${lang.id}/$filename"
      val file = FileOps.getFile(path).get
      Ok.sendFile(content = file, fileName = _ => Some(filename))
    }

  private def parseLang(implicit r: Request[AnyContent]): PrintingLanguage =
    r.getQueryString(ModuleCompendiumController.languageAttribute)
      .flatMap(PrintingLanguage.apply)
      .getOrElse(PrintingLanguage.German)
}
