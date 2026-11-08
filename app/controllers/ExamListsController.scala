package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import auth.AuthorizationAction
import controllers.actions.UserResolveAction
import database.repo.PermissionRepository
import permission.ArtifactCheck
import play.api.libs.json.JsNull
import play.api.libs.json.Json
import play.api.mvc.*
import play.mvc.Http.HeaderNames
import security.ClientErrorResponse
import service.artifact.examlist.ExamListConfig
import service.artifact.examlist.ExamListConfigException
import service.artifact.examlist.ExamListService

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

  def semesters(): Action[AnyContent] = Action.async(_ => service.semesters().map(xs => Ok(Json.toJson(xs))))

  /** Lists published exam lists, by default the latest semester of each PO unless a semester is given. */
  def getAll(po: Option[String], semester: Option[String], latestOnly: Boolean): Action[AnyContent] =
    Action.async(_ => service.listPublished(po, semester, latestOnly).map(xs => Ok(Json.toJson(xs))))

  def entries(id: UUID): Action[AnyContent] =
    Action.async(_ =>
      service
        .entries(id)
        .map(_.fold(NotFound(Json.obj("message" -> s"exam list $id not found")))(xs => Ok(Json.toJson(xs))))
    )

  def configOptions(po: String): Action[AnyContent] =
    auth
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(po))
      .async(_ => service.configOptions(po).map(_.fold(poNotFound(po))(xs => Ok(Json.obj("modules" -> xs)))))

  def getConfig(po: String): Action[AnyContent] =
    auth
      .andThen(resolveUser)
      .andThen(canPreviewArtifact(po))
      .async(_ => service.config(po).map(_.fold(poNotFound(po))(c => Ok(c.getOrElse(JsNull)))))

  def saveConfig(po: String): Action[ExamListConfig] =
    auth(config).andThen(resolveUser).andThen(canCreateArtifact(po)).async { r =>
      service.saveConfig(po, r.body).map(if _ then NoContent else poNotFound(po))
    }

  def getPreview(po: String): Action[ExamListConfig] =
    auth(config).andThen(resolveUser).andThen(canPreviewArtifact(po)).async { r =>
      r.headers.get(HeaderNames.ACCEPT) match {
        case Some(MimeTypes.PDF) =>
          service
            .preview(po, r.body)
            .map(_.fold(poNotFound(po))(pdf => Ok.sendPath(pdf.path, onClose = () => pdf.close()).as(MimeTypes.PDF)))
            .recover(failure(r))
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

  /** Publishes the exam list for the semester of the configuration. */
  def publish(po: String): Action[ExamListConfig] =
    auth(config).andThen(resolveUser).andThen(canCreateArtifact(po)).async { r =>
      service.publish(po, r.body).map(if _ then NoContent else poNotFound(po)).recover(failure(r))
    }

  /** Parses a configuration and rejects it with all validation errors. */
  private def config =
    parse.json[ExamListConfig].validate { c =>
      val errors = ExamListConfig.errors(c)
      Either.cond(errors.isEmpty, c, BadRequest(Json.obj("message" -> errors.mkString("; "))))
    }

  private def poNotFound(po: String) = NotFound(Json.obj("message" -> s"po $po not found"))

  private def failure(r: RequestHeader): PartialFunction[Throwable, Result] = {
    case e: ExamListConfigException => BadRequest(Json.obj("message" -> e.getMessage))
    case NonFatal(e)                => clientErrors.internalServerError(r, e)
  }
}
