package controllers

import auth.AuthorizationAction
import controllers.ModuleDraftController.VersionSchemeHeader
import controllers.actions.{
  ModuleDraftCheck,
  PermissionCheck,
  VersionSchemeAction
}
import controllers.formats.{
  JsonNullWritable,
  ModuleCompendiumProtocolFormat,
  ModuleFormat,
  PipelineErrorFormat
}
import models.{ModuleCompendiumProtocol, User}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.StudyProgramService
import service.{
  ModuleDraftService,
  ModuleReviewService,
  ModuleUpdatePermissionService
}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleDraftController @Inject() (
    cc: ControllerComponents,
    val moduleDraftService: ModuleDraftService,
    val moduleDraftReviewService: ModuleReviewService,
    val auth: AuthorizationAction,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val studyProgramService: StudyProgramService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleCompendiumProtocolFormat
    with PipelineErrorFormat
    with ModuleDraftCheck
    with PermissionCheck
    with ModuleFormat
    with JsonNullWritable {

  def moduleDrafts() =
    auth async { r =>
      val user = User(r.token.username)
      for { // TODO care: business logic in controller
        modules <- moduleUpdatePermissionService.getAllModulesFromUser(user)
        draftsByModules <- moduleDraftService.allByModules(modules.map(_.id))
        draftsByUser <- moduleDraftService.allByUser(user)
      } yield {
        val moduleWithDraft = modules.map { module =>
          val draft = draftsByModules
            .find(_.module == module.id)
            .orElse(draftsByUser.find(_.module == module.id))
          Json.obj(
            "module" -> module,
            "moduleDraft" -> draft,
            "status" -> draft.status()
          )
        }
        Ok(Json.toJson(moduleWithDraft))
      }
    }

  def keys(moduleId: UUID) =
    auth andThen hasPermissionToEditDraft(moduleId) async { _ =>
      moduleDraftService.getByModuleOpt(moduleId).map { draft =>
        val keysToBeReviewed = draft
          .map(_.keysToBeReviewed)
          .getOrElse(Set.empty[String])
        val modifiedKeys =
          draft
            .map(_.modifiedKeys)
            .getOrElse(Set.empty[String])
        Ok(
          Json.obj(
            "keysToBeReviewed" -> keysToBeReviewed,
            "modifiedKeys" -> modifiedKeys
          )
        )
      }
    }

  def createNewModuleDraft() =
    auth(parse.json[ModuleCompendiumProtocol]) andThen
      new VersionSchemeAction(VersionSchemeHeader) async { r =>
        moduleDraftService
          .createNew(r.body, User(r.request.token.username), r.versionScheme)
          .map {
            case Left(err)    => BadRequest(Json.toJson(err))
            case Right(draft) => Ok(Json.toJson(draft))
          }
      }

  def createOrUpdateModuleDraft(moduleId: UUID) =
    auth(parse.json[ModuleCompendiumProtocol]) andThen
      hasPermissionToEditDraft(moduleId) andThen
      new VersionSchemeAction(VersionSchemeHeader) async { r =>
        moduleDraftService
          .createOrUpdate(
            moduleId,
            r.body,
            User(r.request.token.username),
            r.versionScheme
          )
          .map {
            case Left(err) => BadRequest(Json.toJson(err))
            case Right(_)  => NoContent
          }
      }

  /** Deletes the merge request, all reviews, the branch and module draft which
    * are associated with the module id.
    * @param moduleId
    *   ID of the module draft which needs to be deleted
    * @return
    *   204 No Content
    */
  def deleteModuleDraft(moduleId: UUID) =
    auth andThen hasPermissionToEditDraft(moduleId) async { _ =>
      for {
        _ <- moduleDraftReviewService.delete(moduleId)
        _ <- moduleDraftService.delete(moduleId)
      } yield NoContent
    }
}

object ModuleDraftController {
  val VersionSchemeHeader = "Mocogi-Version-Scheme"
}
