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
import controllers.actions.UserResolveAction
import database.repo.core.StudyProgramPersonRepository
import database.repo.PermissionRepository
import models.UniversityRole
import play.api.http.MimeTypes
import play.api.mvc.AbstractController
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import service.exam.ExamLoadService

@Singleton
final class ExamLoadController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    examLoadService: ExamLoadService,
    val permissionRepository: PermissionRepository,
    val studyProgramPersonRepository: StudyProgramPersonRepository,
    @Named("tmp.dir") tmpDir: String,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with DirectorCheck
    with PermissionCheck
    with UserResolveAction {

  private def createCSVFile(): Path = {
    val path = Paths.get(tmpDir).resolve(System.currentTimeMillis().toString)
    Files.createFile(path)
  }

  def generateExamLoad(studyProgram: String, po: String): Action[AnyContent] = auth
    .andThen(resolveUser)
    .andThen(hasRoleInStudyProgram(List(UniversityRole.PAV), studyProgram))
    .async { _ =>
      val file = createCSVFile()
      examLoadService
        .createLatestExamLoad(po)
        .map { csv =>
          Files.writeString(file, csv)
          Ok
            .sendPath(file, onClose = () => Files.delete(file), fileName = _ => Some(s"$po.csv"))
            .as(MimeTypes.TEXT)
        }
        .recoverWith {
          case NonFatal(e) =>
            Files.delete(file)
            Future.failed(e)
        }
    }
}
