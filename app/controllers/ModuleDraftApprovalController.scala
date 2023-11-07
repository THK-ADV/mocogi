package controllers

import auth.{AuthorizationAction, UserTokenRequest}
import controllers.actions.{ApprovalCheck, PermissionCheck}
import controllers.formats.{JsonNullWritable, UserFormat}
import models.{ModuleReview, ModuleReviewStatus, UniversityRole, User}
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ModuleApprovalService.ModuleReviewSummaryStatus.{
  WaitingForChanges,
  WaitingForReview
}
import service.ModuleApprovalService.{
  ModuleReviewSummaryStatus,
  ReviewerApproval
}
import service.{ModuleApprovalService, ModuleReviewService}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

object ModuleDraftApprovalController extends JsonNullWritable with UserFormat {
  implicit val urFmt: Writes[UniversityRole] =
    Writes.of[String].contramap(_.id)

  implicit val sFmt: Writes[ModuleReviewStatus] =
    Writes.of[String].contramap(_.id)

  implicit val fmt: Writes[ModuleReview] =
    Json.writes[ModuleReview]

  implicit val rsfmt: Writes[ModuleReviewSummaryStatus] = {
    case s @ WaitingForChanges =>
      Json.obj(
        "id" -> s.id,
        "deLabel" -> s.deLabel,
        "enLabel" -> s.enLabel
      )
    case s @ WaitingForReview(approved, needed) =>
      Json.obj(
        "id" -> s.id,
        "deLabel" -> s.deLabel,
        "enLabel" -> s.enLabel,
        "approved" -> approved,
        "needed" -> needed
      )
  }

  implicit val rfmt: Writes[ReviewerApproval] =
    Json.writes[ReviewerApproval]

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
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ApprovalCheck
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

  def get(id: UUID) =
    auth async { r =>
      approvalService
        .reviewerApprovals(User(this.user(r)))
        .map(xs => Ok(Json.toJson(xs.find(_.reviewId == id))))
    }

  def update(id: UUID) =
    auth(parse.json(readsUpdate)) andThen
      hasPermissionToApproveReview(id) async { r =>
        reviewService
          .update(id, User(this.user(r)), r.body._1, r.body._2)
          .map(_ => NoContent)
      }
}
