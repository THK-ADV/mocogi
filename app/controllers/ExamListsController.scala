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
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.ExamListRepository
import database.repo.PermissionRepository
import models.ExamList
import models.Semester
import ops.toFuture
import ops.FileOps
import ops.FileOps.deleteDirectory
import ops.FileOps.move
import permission.ArtifactCheck
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.mvc.*
import play.mvc.Http.HeaderNames
import security.ClientErrorResponse
import service.artifact.ExamListService
import service.StudyProgramPrivilegesService

@Singleton
final class ExamListsController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    service: ExamListService,
    @Named("tmp.dir") tmpDir: String,
    @Named("examListFolder") examListFolder: String,
    val permissionRepository: PermissionRepository,
    val studyProgramPrivilegesService: StudyProgramPrivilegesService,
    val examListRepo: ExamListRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ArtifactCheck
    with UserResolveAction {

  def currentSemesters(): Action[AnyContent] =
    Action((r: Request[AnyContent]) => Ok(Json.toJson(Semester.currentAndNext())))

  def getPreview(studyProgram: String, po: String): Action[AnyContent] =
    auth
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(studyProgram))
      .async { r =>
        r.headers.get(HeaderNames.ACCEPT) match {
          case Some(MimeTypes.PDF) =>
            val filename = s"exam_lists_$po"
            val file     = FileOps.createLatexFile(filename, tmpDir)
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
                  Future.successful(clientErrors.internalServerError(r, e))
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
    Action { (_: Request[AnyContent]) =>
      resolveExamListFile(filename) match
        case Some(p) if Files.isRegularFile(p) =>
          Ok.sendFile(content = p.toFile, fileName = f => Some(f.getName))
        case _ => NotFound
    }

  /** Only serves files that lie inside [[examListFolder]] after normalization (path traversal safe). */
  private def resolveExamListFile(filename: String): Option[Path] =
    val trimmed = filename.trim
    if trimmed.isEmpty || trimmed != filename || trimmed.indexOf('\u0000') >= 0 then None
    else {
      val base     = Paths.get(examListFolder).toAbsolutePath.normalize()
      val resolved = base.resolve(trimmed).normalize()
      Option.when(resolved.startsWith(base))(resolved)
    }

  def getAll(): Action[AnyContent] =
    Action.async { (r: Request[AnyContent]) =>
      examListRepo.eachLatest().map(xs => Ok(Json.toJson(xs)))
    }

  // TODO: message that things get overridden

  def replace(studyProgram: String, po: String): Action[(String, LocalDate)] =
    auth(parse.json(createExamListReads))
      .andThen(resolveUser)
      .andThen(canCreateArtifact(studyProgram))
      .async { (r: UserRequest[(String, LocalDate)]) =>
        val (semester, date) = r.body
        val filename         = s"exam_lists_${semester}_$po"
        val file             = FileOps.createLatexFile(filename, tmpDir)
        val semesterObj      = Semester(semester)
        for {
          path <- service.createExamList(po, file, semesterObj, date).recoverWith {
            case NonFatal(e) =>
              file.getParent.deleteDirectory()
              Future.failed(e)
          }
          newPath <- path.move(Paths.get(examListFolder)).toFuture
          _ = file.getParent.deleteDirectory()
          _ <- examListRepo.createOrUpdate(po, semesterObj.id, date, newPath.getFileName.toString)
        } yield NoContent
      }

  private def createExamListReads: Reads[(String, LocalDate)] = js =>
    for {
      semester <- js.\("semester").validate[String]
      date     <- js.\("date").validate[LocalDateTime].map(_.toLocalDate)
    } yield (semester, date)
}
