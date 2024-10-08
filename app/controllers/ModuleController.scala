package controllers

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import auth.AuthorizationAction
import database.view.ModuleViewRepository
import git.api.GitFileDownloadService
import models.ModuleManagement
import models.StudyProgramModuleAssociation
import ops.FutureOps.OptionOps
import play.api.libs.json.JsArray
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.libs.Files.DefaultTemporaryFileCreator
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import printing.PrintingLanguage
import providers.ConfigReader
import service.ModuleDraftService
import service.ModuleService

@Singleton
final class ModuleController @Inject() (
    cc: ControllerComponents,
    service: ModuleService,
    moduleViewRepository: ModuleViewRepository,
    gitFileDownloadService: GitFileDownloadService,
    draftService: ModuleDraftService,
    fileCreator: DefaultTemporaryFileCreator,
    configReader: ConfigReader,
    val auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  implicit def moduleManagementWrites: Writes[ModuleManagement] = Json.writes

  implicit def studyProgramAssocWrites: Writes[StudyProgramModuleAssociation[Iterable[Int]]] =
    Json.writes

  implicit def moduleViewWrites: Writes[ModuleViewRepository#Entry] =
    Json.writes[ModuleViewRepository#Entry]

  def all() =
    Action.async { request =>
      if (request.getQueryString("select").contains("metadata"))
        service
          .allMetadata()
          .map(xs =>
            Ok(JsArray(xs.map {
              case (id, metadata) =>
                Json.obj("id" -> id, "metadata" -> metadata)
            }))
          )
      else if (request.isExtended)
        moduleViewRepository
          .all()
          .map(xs => Ok(Json.toJson(xs)))
      else if (request.getQueryString("type").contains("generic"))
        service
          .allGenericModules()
          .map(xs =>
            Ok(JsArray(xs.map {
              case (module, pos) =>
                Json.toJsObject(module) + ("pos" -> Json.toJson(pos))
            }))
          )
      else
        service
          .allModuleCore(request.queryString)
          .map(xs => Ok(Json.toJson(xs)))
    }

  // GET by ID

  def get(id: UUID) =
    Action.async { _ =>
      service.get(id).map(x => Ok(Json.toJson(x)))
    }

  def getPreview(id: UUID) =
    auth.async { _ =>
      getFromPreview(id).map(x => Ok(Json.toJson(x)))
    }

  def getLatest(id: UUID) =
    auth.async { _ =>
      draftService
        .getByModuleOpt(id)
        /* instead of accessing .data we first parse it to ModuleJSON to consider nullable values,
        then parse it to ModuleProtocol and serialize to JSON
         */
        .map(_.map(d => Json.toJson(d.protocol())))
        .or(getFromPreview(id).map(_.map(module => Json.toJson(module))))
        .map {
          case Some(js) => Ok(js)
          case None     => NotFound
        }
    }

  // Download File

  def getFile(id: UUID) =
    Action((r: Request[AnyContent]) => getFile0(id)(r))

  def getPreviewFile(id: UUID) =
    auth.async { implicit r => getPreviewFile0(id) }

  def getLatestFile(id: UUID) =
    auth.async { implicit r =>
      draftService.hasModuleDraft(id).flatMap {
        case true  => Future.successful(getFile0(id))
        case false => getPreviewFile0(id)
      }
    }

  private def getPreviewFile0(module: UUID)(implicit r: Request[AnyContent]) =
    gitFileDownloadService
      .downloadModuleFromPreviewBranchAsHTML(module)(r.parseLang())
      .map {
        case Some(content) =>
          try {
            val file = fileCreator.create()
            val path = Files.writeString(file, content)
            Ok.sendPath(path, onClose = () => fileCreator.delete(file))
          } catch {
            case NonFatal(e) =>
              ErrorHandler.badRequest(r.toString(), e)
          }
        case None => NotFound
      }

  private def outputFolderPath(lang: PrintingLanguage) =
    lang.fold(configReader.deOutputFolderPath, configReader.enOutputFolderPath)

  private def getFromPreview(moduleId: UUID) =
    gitFileDownloadService.downloadModuleFromPreviewBranch(moduleId)

  private def getFile0(moduleId: UUID)(implicit r: Request[AnyContent]) = {
    val filename = s"$moduleId.html"
    val folder   = outputFolderPath(r.parseLang())
    try {
      val path = Paths.get(s"$folder/$filename")
      Ok.sendFile(content = path.toFile, fileName = f => Some(f.getName))
    } catch {
      case NonFatal(e) =>
        ErrorHandler.badRequest(
          r.toString(),
          s"file not found: ${e.getMessage}"
        )
    }
  }
}
