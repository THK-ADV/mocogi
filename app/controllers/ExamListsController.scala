package controllers

import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import auth.AuthorizationAction
import controllers.actions.UserResolveAction
import database.repo.PermissionRepository
import models.Semester
import permission.ArtifactCheck
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.mvc.*
import play.mvc.Http.HeaderNames
import security.ClientErrorResponse
import service.artifact.ExamListService

@Singleton
final class ExamListsController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    service: ExamListService,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ArtifactCheck
    with UserResolveAction {

  def currentSemesters(): Action[AnyContent] =
    Action(_ => Ok(Json.toJson(service.currentSemesters())))

  def getAll(): Action[AnyContent] =
    Action.async(_ => service.listPublished().map(xs => Ok(Json.toJson(xs))))

  def getPreview(po: String): Action[AnyContent] =
    auth.andThen(resolveUser).andThen(canPreviewArtifact(po)).async { r =>
      r.headers.get(HeaderNames.ACCEPT) match {
        case Some(MimeTypes.PDF) =>
          service
            .preview(po)
            .map(pdf => Ok.sendPath(pdf.path, onClose = () => pdf.close()).as(MimeTypes.PDF))
            .recover {
              case NonFatal(e) => clientErrors.internalServerError(r, e)
            }
        case _ => Future.successful(UnsupportedMediaType(s"expected media type: ${MimeTypes.PDF}"))
      }
    }

  def getFile(filename: String): Action[AnyContent] =
    Action { _ =>
      service.findPublishedFile(filename) match {
        case Some(path) =>
          Ok.sendFile(content = path.toFile, inline = true, fileName = _ => Some(filename)).as(MimeTypes.PDF)
        case _ => NotFound
      }
    }

  def publish(po: String): Action[(Semester, LocalDate)] =
    auth(parse.json(publishReads))
      .andThen(resolveUser)
      .andThen(canCreateArtifact(po))
      .async(r => service.publish(po, r.body._1, r.body._2).map(_ => NoContent))

  private def publishReads: Reads[(Semester, LocalDate)] = js =>
    for {
      semester <- js.\("semester").validate[String]
      date     <- js.\("date").validate[LocalDateTime].map(_.toLocalDate)
    } yield (Semester(semester), date)
}
