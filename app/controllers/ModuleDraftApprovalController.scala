package controllers

import auth.{AuthorizationAction, UserTokenRequest}
import controllers.actions.{ApprovalCheck, ModuleDraftCheck, PermissionCheck}
import controllers.formats.{JsonNullWritable, PersonFormat}
import models.{ModuleReview, User}
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{
  ModuleApprovalService,
  ModuleDraftService,
  ModuleReviewService,
  ModuleUpdatePermissionService
}

import java.util.UUID
import javax.inject.{Inject, Singleton}
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
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ApprovalCheck
    with ModuleDraftCheck
    with PermissionCheck {

  import ModuleDraftApprovalController._

  // TODO DEBUG ONLY
  private def user[A](r: UserTokenRequest[A]) =
    r.getQueryString("user").getOrElse(r.token.username)

  def getOwn =
    auth async { r =>
      approvalService
        .reviewerApprovals(User(this.user(r)))
        .map(xs => Ok(Json.toJson(xs)))
    }

  def getByModule(moduleId: UUID) =
    auth andThen hasPermissionToEditDraft(moduleId) async { _ =>
      reviewService.allByModule(moduleId).map(xs => Ok(Json.toJson(xs)))
    }

  def update(moduleId: UUID, reviewId: UUID) =
    auth(parse.json(readsUpdate)) andThen
      hasPermissionToApproveReview(reviewId) async { r =>
        reviewService
          .update(reviewId, User(this.user(r)), r.body._1, r.body._2)
          .map(_ => NoContent)
      }
}
