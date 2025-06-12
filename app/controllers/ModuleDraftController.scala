package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import controllers.actions.ModuleDraftCheck
import controllers.actions.PermissionCheck
import controllers.actions.PersonAction
import controllers.actions.VersionSchemeAction
import controllers.json.ModuleJson
import controllers.ModuleDraftController.VersionSchemeHeader
import database.repo.core.IdentityRepository
import models.*
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.libs.json.*
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.*
import service.core.StudyProgramService
import service.PipelineError.parsingErrorWrites

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
    val moduleApprovalService: ModuleApprovalService,
    @Named("git.repoUrl") val repoUrl: String,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleDraftCheck
    with PermissionCheck
    with PersonAction
    with JsonNullWritable
    with I18nSupport {

  def moduleDrafts() =
    auth.andThen(personAction).async { r =>
      for {
        modules <- moduleUpdatePermissionService
          .allForCampusId(r.request.campusId)
      } yield Ok(Json.toJson(modules.map {
        case (module, kind, draft) =>
          Json.obj(
            "module" -> module,
            "moduleDraft" -> draft
              .map(moduleDraftWrites(r.messages).writes),
            "moduleDraftState"    -> draft.state(),
            "privilegedForModule" -> kind.isInherited
          )
      }))
    }

  def keys(moduleId: UUID) =
    auth.andThen(personAction).andThen(hasPermissionToViewDraft(moduleId, moduleApprovalService)).async { request =>
      moduleDraftService
        .getByModuleOpt(moduleId)
        .map { draft =>
          val messages = request.messages
          val (reviewed, modified) = draft
            .map(d =>
              (
                d.keysToBeReviewed.map(moduleKeyToJson(_, messages)),
                d.modifiedKeys.map(moduleKeyToJson(_, messages))
              )
            )
            .getOrElse((Set.empty, Set.empty))
          Ok(
            Json.obj(
              "keysToBeReviewed" -> reviewed,
              "modifiedKeys"     -> modified
            )
          )
        }
    }

  def mergeRequestUrl(moduleId: UUID) =
    auth.andThen(personAction).andThen(hasPermissionToViewDraft(moduleId, moduleApprovalService)).async { _ =>
      moduleDraftService
        .getMergeRequestId(moduleId)
        .map {
          case Some(id) => Ok(JsString(repoUrl + s"/-/merge_requests/${id.value}/diffs?view=parallel"))
          case None     => NotFound
        }
    }

  def createNewModuleDraft() =
    auth(parse.json[ModuleJson]).andThen(personAction).andThen(new VersionSchemeAction(VersionSchemeHeader)).async {
      r =>
        moduleDraftService
          .createNew(r.body.toProtocol, r.request.person, r.versionScheme)
          .flatMap {
            case Left(err) => Future.successful(BadRequest(Json.toJson(err)))
            case Right(draft) =>
              for {
                moduleManagement <- identityRepository
                  .getCampusIds(r.body.metadata.moduleManagement.toList)
                _ <- moduleUpdatePermissionService
                  .replace(
                    draft.module,
                    moduleManagement.prepended(r.request.request.campusId),
                    ModuleUpdatePermissionType.Inherited
                  )
              } yield Ok(moduleDraftWrites(r.messages).writes(draft))
          }
    }

  def createOrUpdateModuleDraft(moduleId: UUID) =
    auth(parse.json[ModuleJson])
      .andThen(personAction)
      .andThen(hasPermissionToEditDraft(moduleId))
      .andThen(new VersionSchemeAction(VersionSchemeHeader))
      .async { r =>
        moduleDraftService
          .createOrUpdate(
            moduleId,
            r.body.toProtocol,
            r.request.person,
            r.versionScheme
          )
          .map {
            case Left(err) => BadRequest(Json.toJson(err))
            case Right(_)  => NoContent
          }
      }

  /**
   * Deletes the merge request, all reviews, the branch and module draft which
   * are associated with the module id.
   * @param moduleId
   *   ID of the module draft which needs to be deleted
   * @return
   *   204 No Content
   */
  def deleteModuleDraft(moduleId: UUID) =
    auth.andThen(personAction).andThen(hasPermissionToEditDraft(moduleId)).async { _ =>
      for {
        _ <- moduleDraftReviewService.delete(moduleId)
        _ <- moduleDraftService.delete(moduleId)
      } yield NoContent
    }

  private def moduleKeyToJson(key: String, messages: Messages): JsValue =
    val normalizedKey = key // ModuleKey.normalizeKeyValue(key)
    Json.obj(
      "id"    -> normalizedKey,
      "label" -> messages(s"$normalizedKey.label"),
      "desc"  -> messages(s"$normalizedKey.desc")
    )

  private def moduleDraftWrites(messages: Messages): Writes[ModuleDraft] =
    Writes.apply(d =>
      Json.obj(
        "module" -> d.module,
        "author" -> d.author,
        "status" -> d.source,
        "data"   -> d.moduleJson,
        "keysToBeReviewed" -> d.keysToBeReviewed
          .map(moduleKeyToJson(_, messages)),
        "mergeRequestId" -> d.mergeRequest.map(_._1.value),
        "lastModified"   -> d.lastModified
      )
    )
}

object ModuleDraftController {
  val VersionSchemeHeader = "Mocogi-Version-Scheme"
}
