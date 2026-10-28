package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.PermissionRepository
import git.api.IncompleteComparisonException
import git.CommitId
import permission.AdminCheck
import play.api.libs.json.Json
import play.api.libs.json.JsonValidationError
import play.api.libs.json.Reads
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import play.api.Logging
import security.ClientErrorResponse
import service.SoftBigBangService
import service.SoftBigBangService.InvalidModulesException
import service.SoftBigBangService.ModulesChangedException
import service.SoftBigBangService.PendingException

@Singleton
final class SoftBigBangController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    service: SoftBigBangService,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AdminCheck
    with UserResolveAction
    with Logging {
  import SoftBigBangController.SoftBigBangRequest

  /** Suggests a title for the next soft big bang. */
  def title() =
    auth.andThen(resolveUser).andThen(isAdmin).async { _ =>
      service.title().map(title => Ok(Json.obj("title" -> title)))
    }

  /** Lists the modules that differ between main and preview, together with the compared revisions. */
  def changes() =
    auth.andThen(resolveUser).andThen(isAdmin).async { _ =>
      service
        .changes()
        .map { (main, preview, modules) =>
          val json = modules.map { (id, module, change, file) =>
            Json.obj(
              "id"     -> id,
              "title"  -> module.map(_.title),
              "abbrev" -> module.map(_.abbrev),
              "change" -> change.toString.toLowerCase,
              "file"   -> file
            )
          }
          Ok(Json.obj("mainSha" -> main.value, "previewSha" -> preview.value, "modules" -> json))
        }
        .recover(incompleteComparison)
    }

  /** Publishes the selected modules of the compared revisions via a merge request into main and returns its URL. */
  def create() =
    auth(parse.json[SoftBigBangRequest]).andThen(resolveUser).andThen(isAdmin).async {
      (request: UserRequest[SoftBigBangRequest]) =>
        val body = request.body
        service
          .create(CommitId(body.mainSha), CommitId(body.previewSha), body.modules, body.title.trim, request.person)
          .map(url => Created(Json.obj("mergeRequestUrl" -> url)))
          .recover(incompleteComparison.orElse {
            case e: IllegalArgumentException => clientErrors.badRequest(request, e.getMessage)
            case e: PendingException         => clientErrors.clientStatusError(request, CONFLICT, e.getMessage)
            case e: ModulesChangedException  =>
              logger.warn(s"soft big bang rejected: ${e.getMessage}")
              PreconditionFailed(
                Json.obj("type" -> "modules_changed", "message" -> e.getMessage, "modules" -> e.modules)
              )
            case e: InvalidModulesException =>
              UnprocessableEntity(Json.obj("type" -> "invalid_modules", "message" -> e.getMessage))
          })
    }

  /** Its message is shown to the user, as it neither depends on the request nor exposes internals. */
  private val incompleteComparison: PartialFunction[Throwable, Result] = {
    case e: IncompleteComparisonException =>
      BadGateway(Json.obj("type" -> "incomplete_comparison", "message" -> e.getMessage))
  }
}

object SoftBigBangController {
  final case class SoftBigBangRequest(mainSha: String, previewSha: String, modules: Set[UUID], title: String)

  given Reads[SoftBigBangRequest] =
    Json
      .reads[SoftBigBangRequest]
      .filter(JsonValidationError("Expected commit SHAs, nonempty modules and a title")) { r =>
        Seq(r.mainSha, r.previewSha).forall(_.matches("[0-9a-f]{40}")) && r.modules.nonEmpty && r.title.trim.nonEmpty
      }
}
