package controllers

import java.nio.file.Paths
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success

import auth.AuthorizationAction
import controllers.actions.DirectorCheck
import controllers.actions.PermissionCheck
import controllers.actions.PersonAction
import database.repo.core.IdentityRepository
import database.repo.core.StudyProgramPersonRepository
import database.repo.ModuleCatalogRepository
import models.FullPoId
import models.UniversityRole
import ops.FileOps.FileOps0
import play.api.i18n.Lang
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.Files.DefaultTemporaryFileCreator
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.mvc.Http.HeaderNames
import printing.latex.TextIntroRewriter
import printing.latex.WordTexPrinter
import printing.PrintingLanguage
import service.ModulePreviewService

@Singleton
final class ModuleCatalogController @Inject() (
    cc: ControllerComponents,
    repo: ModuleCatalogRepository,
    fileCreator: DefaultTemporaryFileCreator,
    previewService: ModulePreviewService,
    auth: AuthorizationAction,
    @Named("tmp.dir") tmpDir: String,
    @Named("cmd.word") wordCmd: String,
    @Named("path.mcIntro") mcIntroPath: String,
    val identityRepository: IdentityRepository,
    val studyProgramPersonRepository: StudyProgramPersonRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with DirectorCheck
    with PermissionCheck
    with PersonAction {

  def allFromSemester(semester: String) =
    Action.async(_ => repo.allFromSemester(semester).map(xs => Ok(Json.toJson(xs))))

  def getPreview(studyProgram: String, po: String) =
    auth
      .andThen(personAction)
      .andThen(
        hasRoleInStudyProgram(
          List(UniversityRole.SGL, UniversityRole.PAV),
          studyProgram
        )
      )
      .async { r =>
        r.headers.get(HeaderNames.ACCEPT) match {
          case Some(MimeTypes.PDF) =>
            // german locale
            val pLang    = PrintingLanguage.German
            val lang     = Lang(Locale.GERMANY)
            val filename = s"${pLang.id}_module_catalog_draft_$po"
            val file     = fileCreator.create(filename, ".tex")
            previewService
              .previewCatalog(
                FullPoId(po),
                pLang,
                lang,
                file
              )
              .map(path =>
                Ok.sendPath(
                  path,
                  onClose = () => file.getParentFile.toPath.deleteDirectory()
                ).as(MimeTypes.PDF)
              )
              .recoverWith {
                case NonFatal(e) =>
                  file.getParentFile.toPath.deleteDirectory()
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

  def uploadIntroFile(studyProgram: String) =
    auth
      .andThen(personAction)
      .andThen(
        hasRoleInStudyProgram(
          List(UniversityRole.SGL, UniversityRole.PAV),
          studyProgram
        )
      )
      .apply(parse.json) { (r: PersonAction.PersonRequest[JsValue]) =>
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
