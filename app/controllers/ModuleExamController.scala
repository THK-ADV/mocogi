package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import cats.syntax.traverse.*
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.ModuleExamRepository
import database.repo.PermissionRepository
import models.ModuleExams
import permission.ModuleDraftCheck
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import security.ClientErrorResponse
import service.ModuleUpdatePermissionService

@Singleton
final class ModuleExamController @Inject() (
    cc: ControllerComponents,
    repo: ModuleExamRepository,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val permissionRepository: PermissionRepository,
    auth: AuthorizationAction,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleDraftCheck
    with UserResolveAction {

  def get(module: UUID) =
    auth.andThen(resolveUser).andThen(canEditModule(module)).async { _ =>
      repo.get(module).map(_.fold(notFound(module)) { (title, exams) =>
        Ok(Json.obj("title" -> title, "examNames" -> exams.examNames))
      })
    }

  def replace(module: UUID) =
    auth(parse.json[ModuleExams]).andThen(resolveUser).andThen(canEditModule(module)).async {
      (r: UserRequest[ModuleExams]) =>
        normalize(r.body.examNames) match {
          case Left(message) => Future.successful(BadRequest(Json.obj("message" -> message)))
          case Right(names)  =>
            repo.replace(module, names).map {
              case Some(true)  => Ok(Json.toJson(ModuleExams(names)))
              case Some(false) =>
                BadRequest(Json.obj("message" -> s"module $module is a child or generic module without own exams"))
              case None => notFound(module)
            }
        }
    }

  private def notFound(module: UUID) = NotFound(Json.obj("message" -> s"module $module not found"))

  /** Trims explicit names and requires a non-empty list of non-empty, single-line and unique names. */
  private def normalize(names: Option[List[String]]): Either[String, Option[List[String]]] =
    names.traverse { names =>
      val trimmed = names.map(_.trim)
      if trimmed.isEmpty then Left("exam names must not be empty")
      else if trimmed.exists(n => n.isEmpty || n.exists(c => c == '\n' || c == '\r')) then
        Left("exam names must be non-empty single-line strings")
      else if trimmed.distinct.size != trimmed.size then Left("exam names must be unique")
      else Right(trimmed)
    }
}
