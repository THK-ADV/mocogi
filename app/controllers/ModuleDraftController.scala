package controllers

import auth.AuthorizationAction
import controllers.ModuleDraftController.VersionSchemeHeader
import controllers.actions.{
  ModuleDraftCheck,
  PermissionCheck,
  PersonAction,
  VersionSchemeAction
}
import controllers.formats.{
  JsonNullWritable,
  ModuleCompendiumProtocolFormat,
  ModuleFormat,
  PipelineErrorFormat
}
import database.repo.PersonRepository
import models.{ModuleCompendiumProtocol, ModuleDraft}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.{ModuleKeyService, StudyProgramService}
import service.{
  ModuleCompendiumService,
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
    val personRepository: PersonRepository,
    val moduleCompendiumService: ModuleCompendiumService,
    val moduleKeyService: ModuleKeyService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleCompendiumProtocolFormat
    with PipelineErrorFormat
    with ModuleDraftCheck
    with PermissionCheck
    with ModuleFormat
    with JsonNullWritable
    with PersonAction {

  def moduleDrafts() =
    auth andThen personAction async { r =>
      for { // TODO care: business logic in controller
        modules <- moduleUpdatePermissionService.allForCampusId(
          r.request.campusId
        )
        draftsByModules <- moduleDraftService.allByModules(modules.map(_.id))
        draftsByUser <- moduleDraftService.allByPerson(r.person.id)
      } yield {
        val moduleWithDraft = modules.map { module =>
          val draft = draftsByModules
            .find(_.module == module.id)
            .orElse(draftsByUser.find(_.module == module.id))
          Json.obj(
            "module" -> module,
            "moduleDraft" -> draft,
            "moduleDraftState" -> draft.state()
          )
        }
        Ok(Json.toJson(moduleWithDraft))
      }
    }

  def keys(moduleId: UUID) =
    auth andThen
      personAction andThen
      hasPermissionToEditDraft(moduleId) async { _ =>
        moduleDraftService
          .getByModuleOpt(moduleId)
          .map { draft =>
            val reviewed = draft
              .map(d => moduleKeyService.lookup(d.keysToBeReviewed))
              .getOrElse(Set.empty)
            val modified = draft
              .map(d => moduleKeyService.lookup(d.modifiedKeys))
              .getOrElse(Set.empty)
            Ok(
              Json.obj(
                "keysToBeReviewed" -> reviewed,
                "modifiedKeys" -> modified
              )
            )
          }
      }

  def createNewModuleDraft() =
    auth(parse.json[ModuleCompendiumProtocol]) andThen
      personAction andThen
      new VersionSchemeAction(VersionSchemeHeader) async { r =>
        moduleDraftService
          .createNew(r.body, r.request.person, r.versionScheme)
          .map {
            case Left(err)    => BadRequest(Json.toJson(err))
            case Right(draft) => Ok(Json.toJson(draft))
          }
      }

  def createOrUpdateModuleDraft(moduleId: UUID) =
    auth(parse.json[ModuleCompendiumProtocol]) andThen
      personAction andThen
      hasPermissionToEditDraft(moduleId) andThen
      new VersionSchemeAction(VersionSchemeHeader) async { r =>
        moduleDraftService
          .createOrUpdate(
            moduleId,
            r.body,
            r.request.person,
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
    auth andThen
      personAction andThen
      hasPermissionToEditDraft(moduleId) async { _ =>
        for {
          _ <- moduleDraftReviewService.delete(moduleId)
          _ <- moduleDraftService.delete(moduleId)
        } yield NoContent
      }

  implicit val moduleDraftFmt: Writes[ModuleDraft] =
    Writes.apply(d =>
      Json.obj(
        "module" -> d.module,
        "author" -> d.author,
        "status" -> d.source,
        "data" -> d.data,
        "keysToBeReviewed" -> moduleKeyService.lookup(d.keysToBeReviewed),
        "mergeRequestId" -> d.mergeRequest.map(_._1.value),
        "lastModified" -> d.lastModified
      )
    )
}

object ModuleDraftController {
  val VersionSchemeHeader = "Mocogi-Version-Scheme"
}
