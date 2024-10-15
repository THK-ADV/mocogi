package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import auth.AuthorizationAction
import controllers.actions.DirectorCheck
import controllers.actions.PermissionCheck
import controllers.actions.PersonAction
import database.repo.core.IdentityRepository
import database.repo.core.StudyProgramPersonRepository
import models.FullPoId
import models.UniversityRole
import ops.FileOps.FileOps0
import play.api.libs.Files.DefaultTemporaryFileCreator
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.mvc.Http.HeaderNames
import service.ExamListsPreviewService

@Singleton
final class ExamListsController @Inject() (
    cc: ControllerComponents,
    fileCreator: DefaultTemporaryFileCreator,
    auth: AuthorizationAction,
    previewService: ExamListsPreviewService,
    val identityRepository: IdentityRepository,
    val studyProgramPersonRepository: StudyProgramPersonRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with DirectorCheck
    with PermissionCheck
    with PersonAction {

  def getPreview(studyProgram: String, po: String) =
    auth
      .andThen(personAction)
      .andThen(hasRoleInStudyProgram(List(UniversityRole.PAV), studyProgram))
      .async { r =>
        r.headers.get(HeaderNames.ACCEPT) match {
          case Some(MimeTypes.PDF) =>
            val filename = s"exam_lists_draft_$po"
            val file     = fileCreator.create(filename, ".tex")
            previewService
              .previewExamLists(FullPoId(po), file)
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
}
