package controllers

import auth.AuthorizationAction
import controllers.ModuleDraftController.VersionSchemeHeader
import controllers.actions.{
  ModuleDraftCheck,
  ModuleUpdatePermissionCheck,
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
import service.{ModuleDraftService, ModuleUpdatePermissionService}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleDraftController @Inject() (
    cc: ControllerComponents,
    val moduleDraftService: ModuleDraftService,
    val auth: AuthorizationAction,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleCompendiumProtocolFormat
    with PipelineErrorFormat
    with ModuleUpdatePermissionCheck
    with ModuleDraftCheck
    with PermissionCheck
    with ModuleFormat
    with JsonNullWritable {

  // GET modulesDrafts/own
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

  // POST modulesDrafts
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

  // PUT moduleDrafts/:id
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

  // DELETE moduleDrafts/:id
  def deleteModuleDraft(moduleId: UUID) =
    auth andThen hasPermissionToEditDraft(moduleId) async { _ =>
      moduleDraftService.deleteDraftWithBranch(moduleId).map(_ => NoContent)
    }
}

object ModuleDraftController {
  val VersionSchemeHeader = "Mocogi-Version-Scheme"
}
