package controllers

import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success

import auth.AuthorizationAction
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.JSONRepository
import database.repo.PermissionRepository
import models.Semester
import ops.FileOps
import ops.FileOps.deleteDirectory
import permission.ArtifactCheck
import play.api.libs.json.*
import play.api.libs.Files.TemporaryFile
import play.api.mvc.*
import play.mvc.Http.HeaderNames
import printing.latex.TextIntroRewriter
import printing.latex.WordLatexPrinter
import security.ClientErrorResponse
import settings.AppSettings
import service.artifact.modulecatalog.ModuleCatalogConfig
import service.artifact.modulecatalog.ModuleCatalogConfigException
import service.artifact.modulecatalog.ModuleCatalogService
import service.StudyProgramPrivilegesService

@Singleton
final class ModuleCatalogController @Inject() (
    cc: ControllerComponents,
    catalogService: ModuleCatalogService,
    auth: AuthorizationAction,
    jsonRepo: JSONRepository,
    appSettings: AppSettings,
    studyProgramPrivilegesService: StudyProgramPrivilegesService,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ArtifactCheck
    with UserResolveAction {

  private def tmpDir: String      = appSettings.play.tmpDir
  private def wordCmd: String     = appSettings.pandoc.wordCmd
  private def mcIntroPath: String = appSettings.pandoc.mcIntroPath

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
   * Generates a PDF module catalog for the PO using the configuration in the request body.
   * The optional `preview` query parameter defaults to `true`; `false` generates the current semester's final catalog.
   *
   * @param po for which the module catalog is created
   * @return the generated PDF file
   */
  def generate(po: String): Action[ModuleCatalogConfig] =
    auth(parse.json[ModuleCatalogConfig])
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(po))
      .async { (r: Request[ModuleCatalogConfig]) =>
        r.headers.get(HeaderNames.ACCEPT) match {
          case Some(MimeTypes.PDF) =>
            val isPreview = r.getQueryString("preview").flatMap(_.toBooleanOption).getOrElse(true)
            val filename  = s"module_catalog_$po"
            val file      = FileOps.createLatexFile(filename, tmpDir)
            val path      =
              if isPreview then catalogService.preview(po, file, r.body)
              else catalogService.create(po, file, Semester.of(), r.body)
            path
              .map(path =>
                Ok.sendPath(
                  path,
                  onClose = () => file.getParent.deleteDirectory()
                ).as(MimeTypes.PDF)
              )
              .recover {
                case NonFatal(e) =>
                  file.getParent.deleteDirectory()
                  e match {
                    case e: ModuleCatalogConfigException => clientErrors.badRequest(r, e)
                    case e                               => clientErrors.internalServerError(r, e)
                  }
              }
          case _ =>
            Future.successful(
              UnsupportedMediaType(
                s"expected media type: ${MimeTypes.PDF}"
              )
            )
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
      .async { _ =>
        catalogService.configOptions(po).map(options => Ok(Json.toJson(options)))
      }

  /**
   * Returns metadata for introductory-file directories of POs for which the user can create artifacts.
   *
   * @return JSON array containing each PO ID and its directory's last-modified timestamp
   */
  def getAllIntroFiles(): Action[AnyContent] =
    auth
      .andThen(resolveUser)
      .async { (r: UserRequest[AnyContent]) =>
        studyProgramPrivilegesService
          .getStudyProgramPrivileges(r.person.id, r.permissions)
          .map { privileges =>
            val studyPrograms = privileges.filter(_.canCreate)
            val intros        = ListBuffer[JsValue]()
            for (p <- Files.list(Paths.get(mcIntroPath)).iterator().asScala if Files.isDirectory(p)) {
              studyPrograms.find(_.studyProgram.po.id == p.getFileName.toString) match {
                case Some(sp) =>
                  val lastModified = Files
                    .getLastModifiedTime(p)
                    .toInstant
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime
                  intros += Json.obj(
                    "po"           -> Json.toJson(sp.studyProgram.po.id),
                    "lastModified" -> Json.toJson(lastModified)
                  )
                case None =>
              }
            }
            Ok(JsArray(intros))
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
      .apply(parse.temporaryFile) { (r: Request[TemporaryFile]) =>
        r.contentType match {
          case Some(MimeTypes.WORD) =>
            val printer  = WordLatexPrinter(wordCmd, mcIntroPath)
            val rewriter = TextIntroRewriter()
            printer.toLatex(r.body.path, po).flatMap(rewriter.rewrite) match {
              case Failure(e) =>
                r.body.delete()
                clientErrors.badRequest(r, e)
              case Success(_) =>
                r.body.delete()
                NoContent
            }
          case other =>
            clientErrors.badRequest(
              r,
              s"expected content-type to be ${MimeTypes.WORD}, but was $other"
            )
        }
      }
}
