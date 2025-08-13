package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.ApprovalCheck
import controllers.actions.ModuleDraftCheck
import controllers.actions.PermissionCheck
import controllers.actions.PersonAction
import database.repo.core.IdentityRepository
import play.api.libs.json.*
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.ModuleApprovalService
import service.ModuleDraftService
import service.ModuleReviewService
import service.ModuleUpdatePermissionService

case class ModuleReviewRequest(approved: Boolean, comment: Option[String], reviews: List[UUID])

@Singleton
final class ModuleDraftApprovalController @Inject() (
    cc: ControllerComponents,
    val approvalService: ModuleApprovalService,
    val reviewService: ModuleReviewService,
    val auth: AuthorizationAction,
    val moduleDraftService: ModuleDraftService,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val identityRepository: IdentityRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ApprovalCheck
    with ModuleDraftCheck
    with PermissionCheck
    with PersonAction
    with JsonNullWritable {

  implicit val readsUpdate: Reads[ModuleReviewRequest] =
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
        comment   <- js.\("comment").validateOpt[String]
        reviewIds <- js.\("reviews").validate[List[UUID]]
      } yield ModuleReviewRequest(approved, comment, reviewIds)

  def getOwn =
    auth.andThen(personAction).async { r =>
      approvalService.reviewerApprovals(r.person).map(xs => Ok(Json.toJson(xs)))
    }

  def getByModule(moduleId: UUID) =
    auth.andThen(personAction).andThen(hasPermissionToViewDraft(moduleId, approvalService)).async {
      _ => // TODO this should not be in ModuleDraftCheck
        reviewService.allByModule(moduleId).map(xs => Ok(Json.toJson(xs)))
    }

  def update() =
    auth(parse.json(readsUpdate)).andThen(personAction).andThen(hasPermissionToApproveReview).async { r =>
      val json = r.request.body
      reviewService.update(json.reviews, r.person, json.approved, json.comment).map(_ => NoContent)
    }
}
