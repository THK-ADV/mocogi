package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import database.repo.JSONRepository
import database.view.ModuleViewRepository
import git.api.GitFileService
import ops.or
import play.api.cache.Cached
import play.api.i18n.I18nSupport
import play.api.libs.json.*
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.pipeline.MetadataPipeline
import service.pipeline.Print
import service.ModuleDraftService
import service.ModuleService

@Singleton
final class ModuleController @Inject() (
    cc: ControllerComponents,
    service: ModuleService,
    moduleViewRepository: ModuleViewRepository,
    gitFileDownloadService: GitFileService,
    draftService: ModuleDraftService,
    pipeline: MetadataPipeline,
    jsonRepository: JSONRepository,
    cached: Cached,
    val auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with I18nSupport {

  private enum DataSource:
    case Live
    case All

  private def caching = cached.status(r => r.method + r.uri, 200, 10.minutes)

  def all() =
    caching {
      Action.async { request =>
        val showMetadata: Boolean        = request.getQueryString("select").contains("metadata")
        val showExtendedModules: Boolean = request.isExtended
        val showGenericModules: Boolean  = request.getQueryString("type").contains("generic")
        val showActive: Boolean          = request.getQueryString("active").fold(false)(_ == "true")
        val filteredPO: Option[String]   = request.getQueryString("po")
        val dataSource: DataSource       = request.getQueryString("source").fold(DataSource.Live) {
          case "all"  => DataSource.All
          case "live" => DataSource.Live
          case _      => DataSource.Live
        }

        (showMetadata, showExtendedModules, showGenericModules, showActive, filteredPO, dataSource) match
          case (true, false, false, false, None, DataSource.Live) =>
            service
              .allMetadata()
              .map(xs => Ok(JsArray(xs.map { case (id, metadata) => Json.obj("id" -> id, "metadata" -> metadata) })))
          case (false, true, false, false, None, DataSource.Live) =>
            moduleViewRepository
              .all()
              .map(Ok(_))
          case (false, false, true, false, None, ds) =>
            val modules = ds.match
              case DataSource.Live => service.allGenericModulesWithPOs()
              case DataSource.All  =>
                for
                  live    <- service.allGenericModulesWithPOs()
                  created <- service.allNewlyCreatedGenericModulesWithPOs()
                yield live ++ created
            modules.map(xs =>
              Ok(JsArray(xs.map {
                case (module, pos) =>
                  Json.toJsObject(module) + ("pos" -> Json.toJson(pos))
              }))
            )
          case (false, false, false, false, None, ds) =>
            val includeDrafts = ds match {
              case DataSource.Live => false
              case DataSource.All  => true
            }
            jsonRepository.allModuleCore(includeDrafts).map(Ok(_))
          case (true, false, false, true, Some(po), DataSource.Live) =>
            service
              .allFromPOWithCompanion(po, activeOnly = true)
              .map(xs =>
                Ok(JsArray(xs.map {
                  case (module, companions) =>
                    val js = Json.obj("module" -> module)
                    companions.foldLeft(js) { case (acc, (k, v)) => acc + (k -> v) }
                }))
              )
          case _ => Future.successful(NotFound)
      }
    }

  def allGenericOptions(id: UUID) =
    Action.async(_ => jsonRepository.allGenericModuleOptions(id).map(Ok(_)))

  // GET by ID

  def get(id: UUID) =
    Action.async { r =>
      if (r.isExtended) jsonRepository.getModuleDetails(id).map(_.fold(NotFound)(Ok(_)))
      else if (r.getQueryString("select").contains("lecturers")) service.getLecturers(id).map(x => Ok(Json.toJson(x)))
      else if (r.getQueryString("select").contains("pos")) service.getPOs(id).map(Ok(_))
      else service.get(id).map(x => Ok(Json.toJson(x)))
    }

  def getPreview(id: UUID) =
    auth.async { _ =>
      getFromPreview(id).map(x => Ok(Json.toJson(x)))
    }

  def getLatest(id: UUID) =
    auth.async { _ =>
      draftService
        .getByModuleOpt(id)
        .map(_.map(d => Json.toJson(d.protocol())))
        .or(getFromPreview(id).map(_.map(module => Json.toJson(module))))
        .map {
          case Some(js) => Ok(js)
          case None     => NotFound
        }
    }

  def parseValidate() =
    auth.async(parse.byteString.map(_.utf8String))(r =>
      pipeline.parseValidate(Print(r.body)).map(m => Ok(Json.toJson(m)))
    )

  private def getFromPreview(moduleId: UUID) =
    gitFileDownloadService.downloadModuleFromPreviewBranch(moduleId)
}
