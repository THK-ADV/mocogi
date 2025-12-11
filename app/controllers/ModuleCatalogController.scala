package controllers

import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success

import auth.AuthorizationAction
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.JSONRepository
import database.repo.PermissionRepository
import models.FullPoId
import models.Semester
import ops.FileOps
import ops.FileOps.FileOps0
import permission.ArtifactCheck
import play.api.cache.Cached
import play.api.libs.json.*
import play.api.mvc.*
import play.mvc.Http.HeaderNames
import printing.latex.TextIntroRewriter
import printing.pandoc.WordLatexPrinter
import service.ModuleCatalogService
import service.StudyProgramPrivilegesService

@Singleton
final class ModuleCatalogController @Inject() (
    cc: ControllerComponents,
    catalogService: ModuleCatalogService,
    auth: AuthorizationAction,
    jsonRepo: JSONRepository,
    @Named("tmp.dir") tmpDir: String,
    @Named("cmd.word") wordCmd: String,
    @Named("path.mcIntro") mcIntroPath: String,
    val permissionRepository: PermissionRepository,
    val studyProgramPrivilegesService: StudyProgramPrivilegesService,
    cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ArtifactCheck
    with UserResolveAction {

  /**
   * Returns all active generic modules for the PO
   *
   * @param studyProgram for which SGL or PAV permission must be granted
   * @param po           for which the generic modules are returned
   * @return JSON array of generic modules
   */
  def allGenericModulesForPO(studyProgram: String, po: String): Action[AnyContent] =
    auth
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(studyProgram))
      .async(_ => jsonRepo.getGenericModulesForPO(po).map(Ok(_)))

  /**
   * Generates a module catalog for the PO. A preview catalog is generated if the query parameter is set to true.
   * The body contains a list of generic modules excluded from the generation.
   *
   * @param studyProgram for which SGL or PAV permission must be granted
   * @param po           for which the module catalog is created
   * @return the PDF file
   */
  def generate(studyProgram: String, po: String): Action[List[UUID]] =
    auth(parse.json[List[UUID]])
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(studyProgram))
      .async { (r: Request[List[UUID]]) =>
        r.headers.get(HeaderNames.ACCEPT) match {
          case Some(MimeTypes.PDF) =>
            val isPreview            = r.getQueryString("preview").flatMap(_.toBooleanOption).getOrElse(true)
            val bannedGenericModules = r.body
            val filename             = s"module_catalog_$po"
            val file                 = FileOps.createLatexFile(filename, tmpDir)
            val path =
              if isPreview then catalogService.preview(po, file, bannedGenericModules)
              else
                catalogService.create(
                  po,
                  file,
                  Semester.current(), // assumes current semester
                  bannedGenericModules
                )
            path
              .map(path =>
                Ok.sendPath(
                  path,
                  onClose = () => file.getParent.deleteDirectory()
                ).as(MimeTypes.PDF)
              )
              .recoverWith {
                case NonFatal(e) =>
                  file.getParent.deleteDirectory()
                  Future.failed(e)
              }
          case _ =>
            Future.successful(
              UnsupportedMediaType(
                s"expected media type: ${MimeTypes.PDF}"
              )
            )
        }
      }

  // TODO: rewrite to work with proper uploads
  def uploadIntroFile(studyProgram: String): Action[JsValue] =
    auth
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(studyProgram))
      .apply(parse.json) { (r: UserRequest[JsValue]) =>
        (r.body \ "po").validate[String] match
          case JsSuccess(po, _) =>
            val fullPoId = FullPoId(po)
            // assumes intro is a .docx file
            val wordPath = Paths.get(tmpDir, s"$po.docx")
            val printer  = WordLatexPrinter(wordCmd, mcIntroPath)
            val rewriter = TextIntroRewriter()
            printer.toLatex(wordPath, fullPoId).flatMap(rewriter.rewrite) match
              case Failure(e) =>
                ErrorHandler.badRequest(r.toString, e)
              case Success(_) =>
                NoContent
          case JsError(_) =>
            ErrorHandler.badRequest(r.toString, "expected po id in json")
      }
}
