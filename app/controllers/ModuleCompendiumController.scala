package controllers

import controllers.formats.ModuleCompendiumOutputFormat
import models.Semester
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
import service.{
  ModuleCompendiumLatexActor,
  ModuleCompendiumService,
  ModuleDraftService
}

import java.nio.file.Paths
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

object ModuleCompendiumController {
  private lazy val languageAttribute = "lang"
}

@Singleton
final class ModuleCompendiumController @Inject() (
    cc: ControllerComponents,
    service: ModuleCompendiumService,
    draftService: ModuleDraftService,
    configReader: ConfigReader,
    moduleCompendiumLatexActor: ModuleCompendiumLatexActor,
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

  def getModuleDescriptionFile(id: UUID) =
    Action { implicit r => getFile(s"$id.html") }

  def createModuleCompendium(sem: String, year: String) =
    Action { _ =>
      val semester =
        if (sem == "sose") Semester.summer(year.toInt)
        else Semester.winter(year.toInt)
      moduleCompendiumLatexActor.generateLatexFiles(semester)
      NoContent
    }

  private def getFile(filename: String)(implicit r: Request[AnyContent]) = {
    val lang = parseLang
    val folder = outputFolderPath(lang)
    try {
      val path = Paths.get(s"$folder/$filename")
      Ok.sendFile(content = path.toFile, fileName = f => Some(f.getName))
    } catch {
      case NonFatal(e) =>
        ErrorHandler.internalServerError(
          r.toString(),
          s"file not found: ${e.getMessage}",
          e.getStackTrace
        )
    }
  }

  private def parseLang(implicit r: Request[AnyContent]): PrintingLanguage =
    r.getQueryString(ModuleCompendiumController.languageAttribute)
      .flatMap(PrintingLanguage.apply)
      .getOrElse(PrintingLanguage.German)

  private def outputFolderPath(lang: PrintingLanguage) =
    lang.fold(configReader.deOutputFolderPath, configReader.enOutputFolderPath)
}
