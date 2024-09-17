package controllers

import auth.AuthorizationAction
import controllers.actions.{DirectorCheck, PermissionCheck, PersonAction}
import database.repo.ModuleCatalogRepository
import database.repo.core.{IdentityRepository, StudyProgramPersonRepository}
import models.{FullPoId, UniversityRole}
import ops.FileOps.FileOps0
import play.api.i18n.I18nSupport
import play.api.libs.Files.DefaultTemporaryFileCreator
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import play.mvc.Http.HeaderNames
import service.ModulePreviewService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
final class ModuleCatalogController @Inject() (
    cc: ControllerComponents,
    repo: ModuleCatalogRepository,
    fileCreator: DefaultTemporaryFileCreator,
    previewService: ModulePreviewService,
    auth: AuthorizationAction,
    val identityRepository: IdentityRepository,
    val studyProgramPersonRepository: StudyProgramPersonRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with DirectorCheck
    with PermissionCheck
    with PersonAction
    with I18nSupport {

  def allFromSemester(semester: String) =
    Action.async(_ =>
      repo.allFromSemester(semester).map(xs => Ok(Json.toJson(xs)))
    )

  def getPreview(studyProgram: String, po: String) =
    auth andThen
      personAction andThen
      hasRoleInStudyProgram(
        List(UniversityRole.SGL, UniversityRole.PAV),
        studyProgram
      ) async { r =>
        r.headers.get(HeaderNames.ACCEPT) match {
          case Some(MimeTypes.PDF) =>
            val lang = r.lang.toPrintingLang()
            val filename = s"${lang.id}_draft_$po"
            val file = fileCreator.create(filename, ".tex")
            previewService
              .previewCatalog(
                FullPoId(po),
                lang,
                r.lang,
                file
              )
              .map(path =>
                Ok.sendPath(
                  path,
                  onClose = () => file.getParentFile.toPath.deleteDirectory()
                ).as(MimeTypes.PDF)
              )
              .recoverWith { case NonFatal(e) =>
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
}
