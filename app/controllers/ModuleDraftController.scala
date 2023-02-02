package controllers

import controllers.formats.{ModuleDraftFormat, PipelineErrorFormat, UserBranchFormat}
import models.ModuleDraftProtocol
import play.api.libs.json.{JsArray, JsNull, Json}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{ModuleDraftReviewService, ModuleDraftService}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleDraftController @Inject() (
    cc: ControllerComponents,
    service: ModuleDraftService,
    reviewService: ModuleDraftReviewService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserBranchFormat
    with ModuleDraftFormat
    with PipelineErrorFormat {

  def moduleDrafts(branch: String) =
    Action.async { _ =>
      service.allFromBranch(branch).map(a => Ok(Json.toJson(a)))
    }

  def addModuleDraft() =
    Action.async(parse.json(moduleDraftProtocolFmt)) { r =>
      createOrUpdate(r.body, None)
    }

  def updateModuleDraft(moduleId: UUID) =
    Action.async(parse.json(moduleDraftProtocolFmt)) { r =>
      createOrUpdate(r.body, Some(moduleId))
    }

  def validate(branch: String) =
    Action.async { _ =>
      service.validateDrafts(branch).map { res =>
        val (tag, data) = res match {
          case Left(errs) => ("failure", JsArray(errs.map(e => Json.toJson(e))))
          case Right(_)   => ("success", JsNull)
        }
        Ok(
          Json.obj(
            "tag" -> tag,
            "data" -> data
          )
        )
      }
    }

  def commit(branch: String) =
    Action(parse.json).async { request =>
      val username = request.body.\("username").validate[String].get
      reviewService
        .createReview(branch, username)
        .map(id => Ok(Json.obj("commitId" -> id)))
    }

  def revertCommit(branch: String) =
    Action.async { _ =>
      reviewService
        .revertReview(branch)
        .map(_ => NoContent)
    }

  private def createOrUpdate(
      protocol: ModuleDraftProtocol,
      existingId: Option[UUID]
  ) =
    service
      .createOrUpdate(existingId, protocol.data, protocol.branch)
      .map(d => Ok(Json.toJson(d)))
}
