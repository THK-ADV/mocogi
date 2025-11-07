package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.*
import controllers.json.ModuleJson
import controllers.ModuleDraftController.VersionSchemeHeader
import database.repo.core.IdentityRepository
import database.repo.PermissionRepository
import models.*
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.libs.json.*
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.Logging
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
    val permissionRepository: PermissionRepository,
    val moduleService: ModuleService,
    val moduleApprovalService: ModuleApprovalService,
    @Named("git.repoUrl") val repoUrl: String,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with ModuleDraftCheck
    with PermissionCheck
    with PersonAction
    with JsonNullWritable
    with I18nSupport
    with Logging {

  // /my-modules
  def moduleDrafts() =
    auth.andThen(personAction).async { (r: PersonRequest[AnyContent]) =>
      moduleUpdatePermissionService.allForUser(r.request.campusId, r.permissions).map { moduleJs =>
        var js = moduleJs
        r.accreditationPOs.foreach(accreditationPOs => js = js + ("accreditationPOs" -> Json.toJson(accreditationPOs)))
        r.permissions.modulePermissions.foreach(pos => js = js + ("pos" -> Json.toJson(pos)))
        Ok(js)
      }
    }

  def getModuleDraft(moduleId: UUID) =
    auth.andThen(personAction).andThen(canViewDraft(moduleId, moduleApprovalService)).async { _ =>
      moduleDraftService
        .getByModuleOpt(moduleId)
        .map(draft => Ok(Json.toJson(draft.state())))
    }

  def keys(moduleId: UUID) =
    auth.andThen(personAction).andThen(canViewDraft(moduleId, moduleApprovalService)).async { request =>
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
    auth.andThen(personAction).andThen(canViewDraft(moduleId, moduleApprovalService)).async { _ =>
      moduleDraftService
        .getMergeRequestId(moduleId)
        .map {
          case Some(id) => Ok(JsString(repoUrl + s"/-/merge_requests/${id.value}/diffs?view=parallel"))
          case None     => NotFound
        }
    }

  def createOrUpdateModuleDraft(moduleId: UUID) =
    auth(parse.json[ModuleJson])
      .andThen(personAction)
      .andThen(canUpdateDraft(moduleId, moduleApprovalService))
      .andThen(new VersionSchemeAction(VersionSchemeHeader))
      .async { r =>
        for {
          canApproveModule <- moduleApprovalService.canApproveModule(moduleId, r.request.person.id)
          res <- moduleDraftService
            .createOrUpdate(
              moduleId,
              r.body.toProtocol,
              canApproveModule,
              r.request.person,
              r.versionScheme
            )
        } yield res match {
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
    auth.andThen(personAction).andThen(canDeleteDraft(moduleId)).async { _ =>
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

}

object ModuleDraftController {
  val VersionSchemeHeader = "Mocogi-Version-Scheme"
}
