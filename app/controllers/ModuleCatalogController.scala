package controllers

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success

import auth.AuthorizationAction
import controllers.actions.DirectorCheck
import controllers.actions.PermissionCheck
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.core.StudyProgramPersonRepository
import database.repo.JSONRepository
import database.repo.ModuleCatalogRepository
import database.repo.PermissionRepository
import models.FullPoId
import models.Semester
import models.UniversityRole
import ops.FileOps.FileOps0
import play.api.cache.Cached
import play.api.libs.json.*
import play.api.mvc.*
import play.mvc.Http.HeaderNames
import printing.latex.TextIntroRewriter
import printing.latex.WordTexPrinter
import service.ModulePreviewService

@Singleton
final class ModuleCatalogController @Inject() (
    cc: ControllerComponents,
    repo: ModuleCatalogRepository,
    previewService: ModulePreviewService,
    auth: AuthorizationAction,
    jsonRepo: JSONRepository,
    @Named("tmp.dir") tmpDir: String,
    @Named("cmd.word") wordCmd: String,
    @Named("path.mcIntro") mcIntroPath: String,
    val permissionRepository: PermissionRepository,
    val studyProgramPersonRepository: StudyProgramPersonRepository,
    cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with DirectorCheck
    with PermissionCheck
    with UserResolveAction {

  private def createFile(filename: String): Path = {
    val newDir = Files.createDirectories(Paths.get(tmpDir).resolve(System.currentTimeMillis().toString))
    Files.createFile(newDir.resolve(s"$filename.tex"))
  }

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
      .andThen(
        hasRoleInStudyProgram(
          List(UniversityRole.SGL, UniversityRole.PAV),
          studyProgram
        )
      )
      .async(_ => jsonRepo.getGenericModulesForPO(po).map(Ok(_)))

  def allFromSemester(semester: String): EssentialAction =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action.async(_ => repo.allFromSemester(semester).map(xs => Ok(Json.toJson(xs))))
    }

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
      .andThen(
        hasRoleInStudyProgram(
          List(UniversityRole.SGL, UniversityRole.PAV),
          studyProgram
        )
      )
      .async { (r: Request[List[UUID]]) =>
        r.headers.get(HeaderNames.ACCEPT) match {
          case Some(MimeTypes.PDF) =>
            val isPreview            = r.getQueryString("preview").flatMap(_.toBooleanOption).getOrElse(true)
            val bannedGenericModules = r.body
            val filename             = s"module_catalog_$po"
            val file                 = createFile(filename)
            val path =
              if isPreview then previewService.previewCatalog(po, file, bannedGenericModules)
              else
                previewService.createCatalog(
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
      .andThen(
        hasRoleInStudyProgram(
          List(UniversityRole.SGL, UniversityRole.PAV),
          studyProgram
        )
      )
      .apply(parse.json) { (r: UserRequest[JsValue]) =>
        (r.body \ "po").validate[String] match
          case JsSuccess(po, _) =>
            val fullPoId = FullPoId(po)
            // assumes intro is a .docx file
            val wordPath = Paths.get(tmpDir, s"$po.docx")
            val printer  = WordTexPrinter(wordCmd, mcIntroPath)
            val rewriter = TextIntroRewriter()
            printer.toTex(wordPath, fullPoId).flatMap(rewriter.rewrite) match
              case Failure(e) =>
                ErrorHandler.badRequest(r.toString, e)
              case Success(_) =>
                NoContent
          case JsError(_) =>
            ErrorHandler.badRequest(r.toString, "expected po id in json")
      }
}
