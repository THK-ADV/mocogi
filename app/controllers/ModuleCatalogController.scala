package controllers

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
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
import play.api.cache.Cached
import play.api.libs.json.*
import play.api.libs.Files.TemporaryFile
import play.api.mvc.*
import play.mvc.Http.HeaderNames
import printing.latex.TextIntroRewriter
import printing.latex.WordLatexPrinter
import service.artifact.ModuleCatalogService
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

  /**
   * Returns all introductory files for study programs (POs) for which the user has privileges.
   * The output includes the PO id and the last modified timestamp for each introductory file.
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
   * Handles the upload of an introductory file for a specific study program and PO.
   * The method processes a Word document, converts it to LaTeX, and rewrites specific parts of the content.
   * A user must have the required permissions to perform this action.
   */
  def uploadIntroFile(studyProgram: String, po: String): Action[TemporaryFile] =
    auth
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(studyProgram))
      .apply(parse.temporaryFile) { (r: Request[TemporaryFile]) =>
        r.contentType match {
          case Some(MimeTypes.WORD) =>
            val printer  = WordLatexPrinter(wordCmd, mcIntroPath)
            val rewriter = TextIntroRewriter()
            printer.toLatex(r.body.path, po).flatMap(rewriter.rewrite) match {
              case Failure(e) =>
                r.body.delete()
                ErrorHandler.badRequest(r.toString, e)
              case Success(_) =>
                r.body.delete()
                NoContent
            }
          case other =>
            ErrorHandler.badRequest(r.toString, s"expected content-type to be ${MimeTypes.WORD}, but was $other")
        }
      }
}
