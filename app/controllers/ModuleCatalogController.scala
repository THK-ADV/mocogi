package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success

import auth.AuthorizationAction
import controllers.actions.UserResolveAction
import database.repo.JSONRepository
import database.repo.PermissionRepository
import permission.ArtifactCheck
import play.api.libs.json.*
import play.api.libs.Files.TemporaryFile
import play.api.mvc.*
import play.mvc.Http.HeaderNames
import security.ClientErrorResponse
import service.artifact.modulecatalog.ModuleCatalogConfig
import service.artifact.modulecatalog.ModuleCatalogConfigException
import service.artifact.modulecatalog.ModuleCatalogService

@Singleton
final class ModuleCatalogController @Inject() (
    cc: ControllerComponents,
    catalogService: ModuleCatalogService,
    auth: AuthorizationAction,
    jsonRepo: JSONRepository,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ArtifactCheck
    with UserResolveAction {

  /**
   * Publicly lists the current published module catalog for each non-expired base PO.
   *
   * @return JSON array of catalog metadata, empty when no catalogs are published
   */
  def getAll(): Action[AnyContent] =
    Action.async(_ => catalogService.listPublished().map(xs => Ok(Json.toJson(xs))))

  /**
   * Publicly downloads a published or archived PDF from the configured catalog folder.
   * Only regular files whose resolved paths remain inside that folder are served.
   *
   * @param filename a simple PDF filename without path components
   * @return the PDF download, or 404 for an invalid filename or unavailable file
   */
  def getFile(filename: String): Action[AnyContent] =
    Action { _ =>
      catalogService.findPublishedFile(filename) match
        case Some(path) =>
          Ok.sendFile(content = path.toFile, inline = true, fileName = _ => Some(filename)).as(MimeTypes.PDF)
        case _ => NotFound
    }

  /**
   * Returns the generic modules available for the PO.
   *
   * @param po for which the generic modules are returned
   * @return JSON array of generic modules
   */
  def allGenericModulesForPO(po: String): Action[AnyContent] =
    auth
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(po))
      .async(_ => jsonRepo.getGenericModulesForPO(po).map(Ok(_)))

  /**
   * Generates a temporary PDF preview using the configuration in the request body.
   *
   * @param po for which the module catalog is created
   * @return the generated PDF preview
   */
  def getPreview(po: String): Action[ModuleCatalogConfig] =
    auth(parse.json[ModuleCatalogConfig])
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(po))
      .async { r =>
        r.headers.get(HeaderNames.ACCEPT) match {
          case Some(MimeTypes.PDF) =>
            catalogService
              .preview(po, r.body)
              .map(pdf => Ok.sendPath(pdf.path, onClose = () => pdf.close()).as(MimeTypes.PDF))
              .recover {
                case e: ModuleCatalogConfigException => clientErrors.badRequest(r, e)
                case NonFatal(e)                     => clientErrors.internalServerError(r, e)
              }
          case _ => Future.successful(UnsupportedMediaType(s"expected media type: ${MimeTypes.PDF}"))
        }
      }

  /** Publishes the current semester's catalog and replaces the PO's current database entry. */
  def publish(po: String): Action[ModuleCatalogConfig] =
    auth(parse.json[ModuleCatalogConfig])
      .andThen(resolveUser)
      .andThen(canCreateArtifact(po))
      .async { r =>
        catalogService.publish(po, r.body).map(_ => NoContent).recover {
          case e: ModuleCatalogConfigException => clientErrors.badRequest(r, e)
          case NonFatal(e)                     => clientErrors.internalServerError(r, e)
        }
      }

  /**
   * Returns the available configuration options for generating a module catalog for the PO.
   *
   * @param po for which the module catalog configuration options are returned
   * @return JSON object containing the available configuration options
   */
  def configOptions(po: String): Action[AnyContent] =
    auth
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(po))
      .async(_ => catalogService.configOptions(po).map(options => Ok(Json.toJson(options))))

  /**
   * Returns metadata for introductory-file directories of POs for which the user can create artifacts.
   *
   * @return JSON array containing each PO ID and its directory's last-modified timestamp
   */
  def getAllIntroFiles(): Action[AnyContent] =
    auth.andThen(resolveUser).async { r =>
      catalogService.listIntroFiles(r.person.id, r.permissions).map { intros =>
        Ok(JsArray(intros.map(info => Json.obj("po" -> info.po, "lastModified" -> info.lastModified))))
      }
    }

  /**
   * Converts an uploaded Word introductory file to LaTeX and stores it for the PO.
   *
   * @param po for which the introductory file is stored
   * @return no content on success
   */
  def uploadIntroFile(po: String): Action[TemporaryFile] =
    auth
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(po))
      .apply(parse.temporaryFile) { r =>
        try
          r.contentType match {
            case Some(MimeTypes.WORD) =>
              catalogService.uploadIntroFile(po, r.body.path) match {
                case Failure(e) => clientErrors.badRequest(r, e)
                case Success(_) => NoContent
              }
            case other => clientErrors.badRequest(r, s"expected content-type to be ${MimeTypes.WORD}, but was $other")
          }
        finally r.body.delete()
      }
}
