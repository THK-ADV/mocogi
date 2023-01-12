package controllers

import controllers.json.UserBranchFormat
import models.{ModuleDraft, ModuleDraftStatus}
import play.api.libs.json.{Format, Json}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ModuleDraftService

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleDraftController @Inject() (
    cc: ControllerComponents,
    val service: ModuleDraftService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserBranchFormat {

  case class ModuleDraftProtocol(
      data: String,
      branch: String,
      status: ModuleDraftStatus
  )

  implicit val moduleDraftStatusFmt: Format[ModuleDraftStatus] =
    Format.of[String].bimap(ModuleDraftStatus.apply, _.toString)

  implicit val moduleDraftFmt: Format[ModuleDraft] =
    Json.format[ModuleDraft]

  implicit val moduleDraftProtocolFmt: Format[ModuleDraftProtocol] =
    Json.format[ModuleDraftProtocol]

  def moduleDrafts(branch: String) =
    Action.async { _ =>
      service.allFromBranch(branch).map(a => Ok(Json.toJson(a)))
    }

  def addModuleDraft() =
    Action.async(parse.json(moduleDraftProtocolFmt)) { r =>
      createOrUpdate(r.body, UUID.randomUUID)
    }

  def updateModuleDraft(moduleId: UUID) =
    Action.async(parse.json(moduleDraftProtocolFmt)) { r =>
      createOrUpdate(r.body, moduleId)
    }

  private def createOrUpdate(protocol: ModuleDraftProtocol, id: UUID) = {
    val draft =
      ModuleDraft(
        id,
        protocol.data,
        protocol.branch,
        protocol.status,
        LocalDateTime.now()
      )
    service.update(draft).map(d => Ok(Json.toJson(d)))
  }
}
