package controllers

import auth.AuthorizationAction
import catalog.ModuleCatalogLatexActor
import controllers.actions.{DirectorCheck, PermissionCheck, PersonAction}
import database.repo.ModuleCatalogRepository
import database.repo.core.{IdentityRepository, StudyProgramPersonRepository}
import models.Semester
import ops.FileOps.FileOps0
import play.api.libs.Files.DefaultTemporaryFileCreator
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import play.mvc.Http.HeaderNames
import service.ModulePreviewService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleCatalogController @Inject() (
    cc: ControllerComponents,
    repo: ModuleCatalogRepository,
    actor: ModuleCatalogLatexActor,
    fileCreator: DefaultTemporaryFileCreator,
    previewService: ModulePreviewService,
    auth: AuthorizationAction,
    val identityRepository: IdentityRepository,
    val studyProgramPersonRepository: StudyProgramPersonRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with DirectorCheck
    with PermissionCheck
    with PersonAction {

  def allFromSemester(semester: String) =
    Action.async(_ =>
      repo.allFromSemester(semester).map(xs => Ok(Json.toJson(xs)))
    )

  // TODO DEBUG ONLY. Generation of Module Catalog should be part of a pipeline
  def generate(semester: String) =
    Action { _ =>
      actor.generateLatexFiles(Semester(semester))
      NoContent
    }

  def getPreview(studyProgram: String, po: String) =
    auth andThen
      personAction andThen
      isDirector(studyProgram) async { r =>
        r.headers.get(HeaderNames.ACCEPT) match {
          case Some(MimeTypes.JSON) =>
            previewService.previewModules(po).map { case (preview, publish) =>
              Ok(
                Json.obj(
                  "preview" -> Json.toJson(preview),
                  "publish" -> Json.toJson(publish)
                )
              )
                .as(MimeTypes.JSON)
            }
          case Some(MimeTypes.PDF) =>
            val lang = r.parseLang()
            val filename = s"${lang.id}_draft_$po"
            val file = fileCreator.create(filename, ".tex")
            previewService
              .previewCatalog(po, lang, file)
              .map(path =>
                Ok.sendPath(
                  path,
                  onClose = () => file.getParentFile.toPath.deleteDirectory()
                ).as(MimeTypes.PDF)
              )
          case _ =>
            Future.successful(
              UnsupportedMediaType(
                s"expected media type: ${MimeTypes.JSON} or ${MimeTypes.PDF}"
              )
            )
        }
      }
}
