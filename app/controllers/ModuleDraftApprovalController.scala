package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.annotation.unused
import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.ApprovalCheck
import controllers.actions.ModuleDraftCheck
import controllers.actions.PermissionCheck
import controllers.actions.PersonAction
import database.repo.core.IdentityRepository
import play.api.libs.json._
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.ModuleApprovalService
import service.ModuleDraftService
import service.ModuleReviewService
import service.ModuleUpdatePermissionService

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

  def getOwn =
    auth.andThen(personAction).async { r =>
      approvalService
        .reviewerApprovals(r.person)
        .map(xs => Ok(Json.toJson(xs)))
    }

  def getByModule(moduleId: UUID) =
    auth.andThen(personAction).andThen(hasPermissionToViewDraft(moduleId, approvalService)).async {
      _ => // TODO this should not be in ModuleDraftCheck
        reviewService.allByModule(moduleId).map(xs => Ok(Json.toJson(xs)))
    }

  def update(@unused moduleId: UUID, reviewId: UUID) =
    auth(parse.json(readsUpdate)).andThen(personAction).andThen(hasPermissionToApproveReview(reviewId)).async { r =>
      reviewService
        .update(reviewId, r.person, r.body._1, r.body._2)
        .map(_ => NoContent)
    }
}
