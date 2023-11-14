package controllers

import auth.AuthorizationAction
import controllers.actions.{ApprovalCheck, ModuleDraftCheck, PermissionCheck, PersonAction}
import controllers.formats.{JsonNullWritable, PersonFormat}
import database.repo.PersonRepository
import models.ModuleReview
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{ModuleApprovalService, ModuleDraftService, ModuleReviewService, ModuleUpdatePermissionService}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent.ExecutionContext

object ModuleDraftApprovalController
    extends JsonNullWritable
    with PersonFormat {

  implicit val fmtDb: Writes[ModuleReview.DB] =
    Json.writes[ModuleReview.DB]

  implicit val fmtAtomic: Writes[ModuleReview.Atomic] =
    Json.writes[ModuleReview.Atomic]

  implicit val readsUpdate: Reads[(Boolean, Option[String])] =
    js =>
      for {
        approved <- js.\("action").validate[String].flatMap {
          case "approve" => JsSuccess(true)
          case "reject"  => JsSuccess(false)
          case other =>
            JsError(
              s"expected action to be 'approve' or 'reject', but was $other"
            )
        }
        comment <- js.\("comment").validateOpt[String]
      } yield (approved, comment)
}

@Singleton
final class ModuleDraftApprovalController @Inject() (
    cc: ControllerComponents,
    val approvalService: ModuleApprovalService,
    val reviewService: ModuleReviewService,
    val auth: AuthorizationAction,
    val moduleDraftService: ModuleDraftService,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val personRepository: PersonRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ApprovalCheck
    with ModuleDraftCheck
    with PermissionCheck
    with PersonAction {

  import ModuleDraftApprovalController._

  def getOwn =
    auth andThen personAction async { r =>
      approvalService
        .reviewerApprovals(r.person)
        .map(xs => Ok(Json.toJson(xs)))
    }

  def getByModule(moduleId: UUID) =
    auth andThen
      personAction andThen
      hasPermissionToEditDraft(moduleId) async { _ =>
        reviewService.allByModule(moduleId).map(xs => Ok(Json.toJson(xs)))
      }

  def update(@unused moduleId: UUID, reviewId: UUID) =
    auth(parse.json(readsUpdate)) andThen
      personAction andThen
      hasPermissionToApproveReview(reviewId) async { r =>
        reviewService
          .update(reviewId, r.person, r.body._1, r.body._2)
          .map(_ => NoContent)
      }
}
