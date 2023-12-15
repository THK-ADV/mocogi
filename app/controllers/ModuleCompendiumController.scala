package controllers

import auth.AuthorizationAction
import controllers.formats.ModuleCompendiumOutputFormat
import git.api.GitFileDownloadService
import ops.FutureOps.OptionOps
import play.api.libs.Files.DefaultTemporaryFileCreator
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

import java.nio.file.{Files, Paths}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
final class ModuleCompendiumController @Inject() (
    cc: ControllerComponents,
    service: ModuleCompendiumService,
    draftService: ModuleDraftService,
    configReader: ConfigReader,
    auth: AuthorizationAction,
    fileCreator: DefaultTemporaryFileCreator,
    gitFileDownloadService: GitFileDownloadService,
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

  def getStaging(id: UUID) =
    auth.async { _ =>
      getMcFromStaging(id).map(mc => Ok(Json.toJson(mc)))
    }

  def getLatest(id: UUID) = // TODO only which can edit or which should review
    auth.async { _ =>
      draftService
        .getByModuleOpt(id)
        .map(_.map(_.data))
        .or(getMcFromStaging(id).map(_.map(mc => Json.toJson(mc))))
        .map {
          case Some(js) => Ok(js)
          case None     => NotFound
        }
    }

  def getModuleDescriptionFile(id: UUID) =
    Action { implicit r => getFile(id) }

  def getStagingModuleDescriptionFile(id: UUID) =
    auth.async { implicit r => getFileFromStaging(id) }

  def getLatestModuleDescriptionFile(id: UUID) =
    auth.async { implicit r =>
      draftService.hasModuleDraft(id).flatMap {
        case true  => Future.successful(getFile(id))
        case false => getFileFromStaging(id)
      }
    }

  private def getMcFromStaging(moduleId: UUID) =
    gitFileDownloadService.downloadModuleFromDraftBranch(moduleId)

  private def getFileFromStaging(
      moduleId: UUID
  )(implicit r: Request[AnyContent]) =
    gitFileDownloadService
      .downloadModuleFromDraftBranchAsHTML(moduleId)(r.parseLang())
      .map {
        case Some(content) =>
          try {
            val file = fileCreator.create()
            val path = Files.writeString(file, content)
            Ok.sendPath(path, onClose = () => fileCreator.delete(file))
          } catch {
            case NonFatal(e) =>
              ErrorHandler.internalServerError(
                r.toString(),
                e.getMessage,
                e.getStackTrace
              )
          }
        case None => NotFound
      }

  private def getFile(moduleId: UUID)(implicit r: Request[AnyContent]) = {
    val filename = s"$moduleId.html"
    val folder = outputFolderPath(r.parseLang())
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

  private def outputFolderPath(lang: PrintingLanguage) =
    lang.fold(configReader.deOutputFolderPath, configReader.enOutputFolderPath)
}
