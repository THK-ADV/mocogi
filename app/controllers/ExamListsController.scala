package controllers

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
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
import controllers.actions.PersonAction.PersonRequest
import database.repo.core.IdentityRepository
import database.repo.core.StudyProgramPersonRepository
import database.repo.ExamListRepository
import database.table.ExamListDbEntry
import models.ExamList
import models.Semester
import models.UniversityRole
import ops.EitherOps.EStringThrowOps
import ops.FileOps.FileOps0
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.mvc.AbstractController
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import play.mvc.Http.HeaderNames
import service.ExamListService

@Singleton
final class ExamListsController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    service: ExamListService,
    @Named("tmp.dir") tmpDir: String,
    @Named("examListFolder") examListFolder: String,
    val identityRepository: IdentityRepository,
    val studyProgramPersonRepository: StudyProgramPersonRepository,
    val examListRepo: ExamListRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with DirectorCheck
    with PermissionCheck
    with PersonAction {

  private def createExamListFile(filename: String): Path = {
    val newDir = Files.createDirectories(Paths.get(tmpDir).resolve(System.currentTimeMillis().toString))
    Files.createFile(newDir.resolve(s"$filename.tex"))
  }

  def currentSemesters(): Action[AnyContent] =
    Action((r: Request[AnyContent]) => Ok(Json.toJson(Semester.currentAndNext())))

  def getPreview(studyProgram: String, po: String): Action[AnyContent] =
    auth
      .andThen(personAction)
      .andThen(hasRoleInStudyProgram(List(UniversityRole.PAV), studyProgram))
      .async { r =>
        r.headers.get(HeaderNames.ACCEPT) match {
          case Some(MimeTypes.PDF) =>
            val filename = s"exam_lists_$po"
            val file     = createExamListFile(filename)
            service
              .previewExamList(po, file)
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

  def getFile(filename: String): Action[AnyContent] =
    Action { (r: Request[AnyContent]) =>
      val p = Paths.get(examListFolder).resolve(filename)
      if Files.exists(p) then Ok.sendFile(content = p.toFile, fileName = f => Some(f.getName))
      else NotFound
    }

  def getAll(): Action[AnyContent] =
    Action.async { (r: Request[AnyContent]) =>
      examListRepo.eachLatest().map(xs => Ok(Json.toJson(xs)))
    }

  // TODO: message that things get overridden

  def replace(studyProgram: String, po: String): Action[(String, LocalDate)] =
    auth(parse.json(createExamListReads))
      .andThen(personAction)
      .andThen(hasRoleInStudyProgram(List(UniversityRole.PAV), studyProgram))
      .async { (r: PersonRequest[(String, LocalDate)]) =>
        val (semester, date) = r.body
        val filename         = s"exam_lists_${semester}_$po"
        val file             = createExamListFile(filename)
        val semesterObj      = Semester(semester)
        for {
          path <- service.createExamList(po, file, semesterObj, date).recoverWith {
            case NonFatal(e) =>
              file.getParent.deleteDirectory()
              Future.failed(e)
          }
          newPath <- path.move(Paths.get(examListFolder)).toFuture
          _ = file.getParent.deleteDirectory()
          _ <- examListRepo.createOrUpdate(ExamListDbEntry(po, semesterObj.id, date, newPath.getFileName.toString))
        } yield NoContent
      }

  private def createExamListReads: Reads[(String, LocalDate)] = js =>
    for {
      semester <- js.\("semester").validate[String]
      date     <- js.\("date").validate[LocalDateTime].map(_.toLocalDate)
    } yield (semester, date)
}
