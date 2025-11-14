package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.ModuleDraftCheck
import controllers.actions.ModuleReviewCheck
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.PermissionRepository
import models.ModuleKeysToReview
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.Logging
import service.ModuleReviewService
import service.ModuleUpdatePermissionService

case class ModuleReviewRequest(approved: Boolean, comment: Option[String], reviews: List[UUID])

@Singleton
final class ModuleDraftReviewController @Inject() (
    cc: ControllerComponents,
    val auth: AuthorizationAction,
    val moduleReviewService: ModuleReviewService,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val permissionRepository: PermissionRepository,
    private val keysToReview: ModuleKeysToReview,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleDraftCheck
    with ModuleReviewCheck
    with UserResolveAction
    with Logging
    with JsonNullWritable {

  given Reads[ModuleReviewRequest] =
    js =>
      for {
        approved <- js.\("action").validate[String].flatMap {
          case "approve" => JsSuccess(true)
          case "reject"  => JsSuccess(false)
          case other     => JsError(s"expected action to be 'approve' or 'reject', but was $other")
        }
        comment   <- js.\("comment").validateOpt[String]
        reviewIds <- js.\("reviews").validate[List[UUID]]
      } yield ModuleReviewRequest(approved, comment, reviewIds)

  // Returns module reviews of the user
  def moduleReviews() =
    auth.andThen(resolveUser).async { (r: UserRequest[AnyContent]) =>
      moduleReviewService.moduleReviews(r.person, r.permissions).map(xs => Ok(Json.toJson(xs)))
    }

  // Creates a full review for the module draft
  def create(moduleId: UUID) =
    auth.andThen(resolveUser).andThen(canEditModule(moduleId)).async { (r: UserRequest[AnyContent]) =>
      moduleReviewService.create(moduleId, r.person, r.permissions).map(_ => Created)
    }

  // Deletes a full review for the module draft
  def delete(moduleId: UUID) =
    auth.andThen(resolveUser).andThen(canEditModule(moduleId)).async { _ =>
      moduleReviewService.delete(moduleId).map(_ => NoContent)
    }

  // Returns reviews for a module
  def getForModule(moduleId: UUID) =
    auth.andThen(resolveUser).andThen(canEditModule(moduleId)).async { _ =>
      moduleReviewService.allByModule(moduleId).map(xs => Ok(Json.toJson(xs)))
    }

  // Perform a module review
  def update() =
    auth(parse.json[ModuleReviewRequest]).andThen(resolveUser).andThen(canReviewModule).async {
      (r: UserRequest[ModuleReviewRequest]) =>
        val moduleReview = r.request.body
        moduleReviewService
          .update(moduleReview.reviews, r.person, moduleReview.approved, moduleReview.comment)
          .map(_ => NoContent)
    }

  def keys() =
    Action {
      val json = keysToReview.pav.map { key =>
        val label = key match {
          case "metadata.assessmentMethods.mandatory" => "Studiengangszuordnung als Pflichtmodul"
          case "metadata.title"                       => "Modulbezeichnung"
          case "metadata.ects"                        => "Credits (ECTS)"
          case "metadata.moduleManagement"            => "Modulverantwortung"
          case "metadata.examiner.first"              => "Erstprüfer*in"
          case "metadata.examiner.second"             => "Zweitprüfer*in"
          case "metadata.examPhases"                  => "Prüfungsphasen"
          case "metadata.attendanceRequirement"       => "Anwesenheitspflicht"
          case "metadata.assessmentPrerequisite"      => "Prüfungsvorleistung"
          case other =>
            logger.error(s"missing label for key: $other")
            "???"
        }
        Json.obj("id" -> key, "label" -> label)
      }
      Ok(Json.toJson(json))
    }
}
