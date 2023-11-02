package controllers

import auth.{AuthorizationAction, UserTokenRequest}
import controllers.formats.{JsonNullWritable, UserFormat}
import models.{ModuleReview, ModuleReviewStatus, UniversityRole, User}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import service.ModuleApprovalService
import service.ModuleApprovalService.{
  ModuleReviewSummaryStatus,
  ReviewerApproval,
  WaitingForChanges,
  WaitingForReview
}

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
}

@Singleton
final class ModuleDraftApprovalController @Inject() (
    cc: ControllerComponents,
    val service: ModuleApprovalService,
    val auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  import ModuleDraftApprovalController._

  // TODO remove
  private def user(r: UserTokenRequest[AnyContent]) =
    r.getQueryString("user").getOrElse(r.token.username)

  def getOwn =
    auth async { r =>
      service
        .reviewerApprovals(User(this.user(r)))
        .map(xs => Ok(Json.toJson(xs)))
    }

  def get(id: UUID) =
    auth async { r =>
      service
        .reviewerApprovals(User(this.user(r)))
        .map(xs => Ok(Json.toJson(xs.find(_.reviewId == id))))
    }

  def update(id: UUID) =
    auth async { _ =>
      ???
    }
}
