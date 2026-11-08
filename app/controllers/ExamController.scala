package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.ExamRepository
import database.repo.PermissionRepository
import permission.ExamPlanningCheck
import permission.ModuleDraftCheck
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.mvc.*
import security.ClientErrorResponse
import service.ModuleUpdatePermissionService

/**
 * Exams and their module assignments, managed with exam planning privileges. Module editors may read the exams of
 * their module.
 */
@Singleton
final class ExamController @Inject() (
    cc: ControllerComponents,
    repo: ExamRepository,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserResolveAction
    with ExamPlanningCheck
    with ModuleDraftCheck {

  import ExamController.*

  def all() =
    auth.andThen(resolveUser).andThen(canUpdateExamPlan).async((_: UserRequest[AnyContent]) => repo.all().map(Ok(_)))

  def forModule(module: UUID) =
    auth.andThen(resolveUser).andThen(canEditModule(module)).async { (_: UserRequest[AnyContent]) =>
      repo.moduleExams(module).map(_.fold(NotFound(Json.obj("message" -> s"module $module not found")))(Ok(_)))
    }

  def create() =
    auth(parse.json[ExamInput]).andThen(resolveUser).andThen(canUpdateExamPlan).async { (r: UserRequest[ExamInput]) =>
      validated(r.body)((name, modules) => repo.create(name, modules).map(Created(_)))
    }

  def update(id: UUID) =
    auth(parse.json[ExamInput]).andThen(resolveUser).andThen(canUpdateExamPlan).async { (r: UserRequest[ExamInput]) =>
      validated(r.body)((name, modules) => repo.update(id, name, modules).map(_.fold(notFound(id))(Ok(_))))
    }

  def delete(id: UUID) =
    auth.andThen(resolveUser).andThen(canUpdateExamPlan).async { (_: UserRequest[AnyContent]) =>
      repo.delete(id).map {
        case Some(true)  => NoContent
        case Some(false) =>
          Conflict(Json.obj("message" -> s"exam $id is referenced by an exam appointment or a published exam list"))
        case None => notFound(id)
      }
    }

  def merge() =
    auth(parse.json[ExamMerge]).andThen(resolveUser).andThen(canUpdateExamPlan).async { (r: UserRequest[ExamMerge]) =>
      val m = r.body
      if m.sources.isEmpty || m.sources.contains(m.target) then
        Future.successful(badRequest("sources must not be empty or contain the target"))
      else
        name(m.name).fold(
          e => Future.successful(badRequest(e)),
          n =>
            repo
              .merge(m.target, m.sources.toSet, n)
              .map(_.fold(NotFound(Json.obj("message" -> "exam not found")))(Ok(_)))
        )
    }

  private def validated(input: ExamInput)(f: (String, Set[UUID]) => Future[Result]): Future[Result] =
    if input.modules.isEmpty then Future.successful(badRequest("modules must not be empty"))
    else
      name(input.name).fold(
        e => Future.successful(badRequest(e)),
        n => f(n, input.modules.toSet).recover { case e: IllegalArgumentException => badRequest(e.getMessage) }
      )

  private def badRequest(message: String) = BadRequest(Json.obj("message" -> message))

  private def notFound(id: UUID) = NotFound(Json.obj("message" -> s"exam $id not found"))
}

object ExamController {
  final case class ExamInput(name: String, modules: List[UUID])

  final case class ExamMerge(target: UUID, sources: List[UUID], name: String)

  given Reads[ExamInput] = Json.reads

  given Reads[ExamMerge] = Json.reads

  /** Trims the name and requires it to be non-empty and single-line. */
  def name(raw: String): Either[String, String] = {
    val trimmed = raw.trim
    Either.cond(
      trimmed.nonEmpty && !trimmed.exists(c => c == '\n' || c == '\r'),
      trimmed,
      "name must be a non-empty single-line string"
    )
  }
}
