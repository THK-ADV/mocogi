package controllers

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import auth.AuthorizationAction
import auth.Role.AccessDraftBranch
import controllers.actions.PermissionCheck
import controllers.actions.RoleCheck
import database.view.ModuleViewRepository
import git.api.GitFileDownloadService
import git.api.GitRepositoryApiService
import models.ModuleManagement
import models.StudyProgramModuleAssociation
import ops.FutureOps.OptionOps
import play.api.cache.Cached
import play.api.i18n.I18nSupport
import play.api.libs.json.JsArray
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.libs.Files.DefaultTemporaryFileCreator
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import printing.html.ModuleHTMLPrinter
import printing.pandoc.PrinterOutput
import printing.pandoc.PrinterOutputType
import printing.PrintingLanguage
import providers.ConfigReader
import service.MetadataPipeline
import service.ModuleDraftService
import service.ModuleService
import service.Print

@Singleton
final class ModuleController @Inject() (
    cc: ControllerComponents,
    service: ModuleService,
    moduleViewRepository: ModuleViewRepository,
    gitFileDownloadService: GitFileDownloadService,
    gitRepositoryApiService: GitRepositoryApiService,
    draftService: ModuleDraftService,
    fileCreator: DefaultTemporaryFileCreator,
    configReader: ConfigReader,
    pipeline: MetadataPipeline,
    printer: ModuleHTMLPrinter,
    cached: Cached,
    val auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with I18nSupport
    with PermissionCheck
    with RoleCheck {

  implicit def moduleManagementWrites: Writes[ModuleManagement] = Json.writes

  implicit def studyProgramAssocWrites: Writes[StudyProgramModuleAssociation[Iterable[Int]]] =
    Json.writes

  implicit def moduleViewWrites: Writes[ModuleViewRepository#Entry] =
    Json.writes[ModuleViewRepository#Entry]

  private def caching = cached.status(r => r.method + r.uri, 200, 1.hour)

  def all() =
    caching {
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
            .allModuleCore()
            .map(xs => Ok(Json.toJson(xs)))
      }
    }

  def allFromPreview() =
    auth.andThen(hasRole(AccessDraftBranch)).async { _ =>
      val config = gitRepositoryApiService.config
      val branch = config.draftBranch
      /* TODO: this can be optimized by only downloading files from preview which
          has changed (diff). The other files can be obtained from the db */
      for
        paths <- gitRepositoryApiService.listModuleFiles(branch)
        modules <- Future
          .sequence(paths.par.collect {
            case path if path.isModule(config) =>
              gitFileDownloadService
                .downloadModuleMetadataFromPreviewBranch(path)
                .collect { case Some((id, module)) => Json.toJsObject(module) + ("id" -> Json.toJson(id)) }
          }.seq)
      yield Ok(JsArray(modules))
    }

  // GET by ID

  def get(id: UUID) =
    caching {
      Action.async { _ =>
        service.get(id).map(x => Ok(Json.toJson(x)))
      }
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
    Action((r: Request[AnyContent]) => getStaticFile(id)(r))

  def getPreviewFile(id: UUID) =
    auth.async { implicit r => getPreviewFile0(id) }

  def getLatestFile(id: UUID) =
    auth.async { implicit r =>
      draftService.getByModuleOpt(id).flatMap {
        case Some(draft) => printHtml(draft.print, r.lang.toPrintingLang()).map(respondWithFile)
        case None        => getPreviewFile0(id)
      }
    }

  private def printHtml(print: Print, lang: PrintingLanguage): Future[String] =
    for
      module <- pipeline.parseValidate(print)
      output <- printer.print(module, lang, None, PrinterOutputType.HTMLStandalone)
      res <- output match {
        case Left(err)                          => Future.failed(err)
        case Right(PrinterOutput.Text(c, _, _)) => Future.successful(c)
        case Right(PrinterOutput.File(_, _))    => Future.failed(new Exception("expected standalone HTML, but was a file"))
      }
    yield res

  private def respondWithFile(content: String)(implicit r: Request[AnyContent]) =
    try {
      val file = fileCreator.create()
      val path = Files.writeString(file, content)
      Ok.sendPath(path, onClose = () => fileCreator.delete(file))
    } catch {
      case NonFatal(e) =>
        ErrorHandler.badRequest(r.toString(), e)
    }

  private def getPreviewFile0(module: UUID)(implicit r: Request[AnyContent]) =
    gitFileDownloadService
      .downloadModuleFromPreviewBranchAsHTML(module)(r.parseLang())
      .map {
        case Some(content) => respondWithFile(content)
        case None          => NotFound
      }

  private def outputFolderPath(lang: PrintingLanguage) =
    lang.fold(configReader.deOutputFolderPath, configReader.enOutputFolderPath)

  private def getFromPreview(moduleId: UUID) =
    gitFileDownloadService.downloadModuleFromPreviewBranch(moduleId)

  private def getStaticFile(moduleId: UUID)(implicit r: Request[AnyContent]) = {
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
