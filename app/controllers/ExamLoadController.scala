package controllers

import java.nio.file.Files
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import auth.AuthorizationAction
import controllers.actions.UserResolveAction
import database.repo.PermissionRepository
import ops.FileOps
import permission.ArtifactCheck
import play.api.http.ContentTypes
import play.api.mvc.AbstractController
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Codec
import play.api.mvc.ControllerComponents
import security.ClientErrorResponse
import settings.AppSettings
import service.artifact.ExamLoadService
import service.StudyProgramPrivilegesService

@Singleton
final class ExamLoadController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    examLoadService: ExamLoadService,
    val permissionRepository: PermissionRepository,
    val studyProgramPrivilegesService: StudyProgramPrivilegesService,
    appSettings: AppSettings,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ArtifactCheck
    with UserResolveAction {

  private def tmpDir: String = appSettings.play.tmpDir

  def generateExamLoad(studyProgram: String, po: String): Action[AnyContent] =
    auth
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(studyProgram))
      .async { _ =>
        val file = FileOps.createRandomFile(tmpDir)
        examLoadService
          .createLatestExamLoad(po)
          .map { csv =>
            Files.writeString(file, csv)
            Ok
              .sendPath(file, onClose = () => Files.delete(file), fileName = _ => Some(s"$po.csv"))
              .as(ContentTypes.withCharset("text/csv")(Codec.utf_8))
          }
          .recoverWith {
            case NonFatal(e) =>
              Files.delete(file)
              Future.failed(e)
          }
      }
}
