package controllers

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Named
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
import models.UniversityRole
import ops.FileOps.FileOps0
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.mvc.Http.HeaderNames
import service.ExamListService

@Singleton
final class ExamListsController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    service: ExamListService,
    @Named("tmp.dir") tmpDir: String,
    val identityRepository: IdentityRepository,
    val studyProgramPersonRepository: StudyProgramPersonRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with DirectorCheck
    with PermissionCheck
    with PersonAction {

  def getPreview(studyProgram: String, po: String) =
    get0(studyProgram, po)((po, file) => service.previewExamLists(po, file))

  def get(studyProgram: String, po: String) =
    get0(studyProgram, po)((po, file) => service.examLists(po, file))

  private def get0(studyProgram: String, po: String)(createPDF: (String, Path) => Future[Path]) =
    auth
      .andThen(personAction)
      .andThen(hasRoleInStudyProgram(List(UniversityRole.PAV), studyProgram))
      .async { r =>
        r.headers.get(HeaderNames.ACCEPT) match {
          case Some(MimeTypes.PDF) =>
            val filename = s"exam_lists_draft_$po"
            val newDir   = Files.createDirectories(Paths.get(tmpDir).resolve(System.currentTimeMillis().toString))
            val file     = Files.createFile(newDir.resolve(s"$filename.tex"))
            createPDF(po, file)
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
}
