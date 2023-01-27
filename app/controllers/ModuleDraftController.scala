package controllers

import controllers.formats.{ModuleDraftFormat, UserBranchFormat}
import models.ModuleDraftProtocol
import play.api.libs.json.{JsArray, JsNull, Json, Writes}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{ModuleDraftService, PipelineError}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleDraftController @Inject() (
    cc: ControllerComponents,
    service: ModuleDraftService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserBranchFormat
    with ModuleDraftFormat {

  implicit val errWrites: Writes[PipelineError] =
    Writes.apply {
      case PipelineError.Parser(value) =>
        Json.obj(
          "tag" -> "parsing-error",
          "error" -> Json.obj(
            "found" -> value.found,
            "expected" -> value.expected
          )
        )
      case PipelineError.Printer(value) =>
        Json.obj(
          "tag" -> "printing-error",
          "error" -> Json.obj(
            "found" -> value.found,
            "expected" -> value.expected
          )
        )
      case PipelineError.Validator(value) =>
        Json.obj(
          "tag" -> "validation-error",
          "error" -> Json.obj(
            "id" -> value.id,
            "title" -> value.title,
            "errors" -> Json.toJson(value.errs)
          )
        )
    }

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

  private def createOrUpdate(
      protocol: ModuleDraftProtocol,
      existingId: Option[UUID]
  ) =
    service
      .createOrUpdate(existingId, protocol.data, protocol.branch)
      .map(d => Ok(Json.toJson(d)))
}
