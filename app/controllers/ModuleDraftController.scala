package controllers

import auth.AuthorizationAction
import controllers.ModuleDraftController.VersionSchemeHeader
import controllers.actions.{
  ModuleDraftCheck,
  PermissionCheck,
  PersonAction,
  VersionSchemeAction
}
import database.repo.core.IdentityRepository
import models.{ModuleDraft, ModuleProtocol}
import play.api.Logging
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.PipelineError.parsingErrorWrites
import service.core.{ModuleKeyService, StudyProgramService}
import service.{
  ModuleDraftService,
  ModuleReviewService,
  ModuleService,
  ModuleUpdatePermissionService
}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleDraftController @Inject() (
    cc: ControllerComponents,
    val moduleDraftService: ModuleDraftService,
    val moduleDraftReviewService: ModuleReviewService,
    val auth: AuthorizationAction,
    val moduleUpdatePermissionService: ModuleUpdatePermissionService,
    val studyProgramService: StudyProgramService,
    val identityRepository: IdentityRepository,
    val moduleService: ModuleService,
    val moduleKeyService: ModuleKeyService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleDraftCheck
    with PermissionCheck
    with PersonAction
    with JsonNullWritable
    with Logging {

  def moduleDrafts() =
    auth andThen personAction async { r =>
      for {
        modules <- moduleUpdatePermissionService
          .allForCampusId(r.request.campusId)
      } yield Ok(Json.toJson(modules.map { case (module, kind, draft) =>
        Json.obj(
          "module" -> module,
          "moduleDraft" -> draft,
          "moduleDraftState" -> draft.state(),
          "privilegedForModule" -> kind.isInherited
        )
      }))
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
    auth(parse.json[ModuleProtocol]) andThen
      personAction andThen
      new VersionSchemeAction(VersionSchemeHeader) async { r =>
        moduleDraftService
          .createNew(r.body, r.request.person, r.versionScheme)
          .flatMap {
            case Left(err) => Future.successful(BadRequest(Json.toJson(err)))
            case Right(draft) =>
              moduleUpdatePermissionService
                .createOrUpdateInherited(
                  Seq((draft.module, List(r.request.person)))
                )
                .map(_ => Ok(Json.toJson(draft)))
          }
      }

  def createOrUpdateModuleDraft(moduleId: UUID) =
    auth(parse.json[ModuleProtocol]) andThen
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
