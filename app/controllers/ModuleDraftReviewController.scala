package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.ModuleDraftCheck
import controllers.actions.PermissionCheck
import controllers.actions.PersonAction
import database.repo.core.IdentityRepository
import models.ModuleKeysToReview
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import play.api.Logging
import service.ModuleDraftService
import service.ModuleReviewService
import service.ModuleUpdatePermissionService

@Singleton
final class ModuleDraftReviewController @Inject() (
    cc: ControllerComponents,
    val auth: AuthorizationAction,
    val service: ModuleReviewService,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val moduleDraftService: ModuleDraftService,
    val identityRepository: IdentityRepository,
    private val keysToReview: ModuleKeysToReview,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleDraftCheck
    with PermissionCheck
    with PersonAction
    with Logging {

  /**
   * Creates a full review.
   * @param moduleId
   *   ID of the module draft
   * @return
   *   201 Created
   */
  def create(moduleId: UUID) =
    auth.andThen(personAction).andThen(hasPermissionToEditDraft(moduleId)).async { r =>
      moduleInReaccreditation(moduleId, r)
        .flatMap(fastForward =>
          if fastForward then service.createAutoAccepted(moduleId, r.person) else service.create(moduleId, r.person)
        )
        .map(_ => Created)
    }

  /**
   * Deletes a full review.
   * @param moduleId
   *   ID of the module draft
   * @return
   *   204 NoContent
   */
  def delete(moduleId: UUID) =
    auth.andThen(personAction).andThen(hasPermissionToEditDraft(moduleId)).async { _ =>
      service.delete(moduleId).map(_ => NoContent)
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
