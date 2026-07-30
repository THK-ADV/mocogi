package service

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import database.repo.ModuleDraftRepository
import git.api.GitBranchService
import git.api.GitCommitService
import git.api.GitFileService
import git.MergeRequestId
import logging.errorC
import logging.infoC
import logging.CorrelationId
import models.*
import models.core.Identity
import ops.continueIf
import parsing.metadata.VersionScheme
import parsing.types.*
import play.api.libs.json.*
import play.api.Logging
import service.modulediff.ModuleProtocolDiff.diff
import service.modulediff.ModuleProtocolDiff.nonEmptyKeys
import service.pipeline.MetadataPipeline
import service.pipeline.PipelineError

case class ModuleUpdateRequest(
    moduleId: UUID,
    protocol: ModuleProtocol,
    person: Identity.Person,
    canApproveModule: Boolean,
    versionScheme: VersionScheme
)

@Singleton
final class ModuleDraftService @Inject() (
    val repo: ModuleDraftRepository,
    private val gitBranchService: GitBranchService,
    private val gitCommitService: GitCommitService,
    private val keysToReview: ModuleKeysToReview,
    private val gitFileDownloadService: GitFileService,
    private val pipeline: MetadataPipeline,
    private implicit val ctx: ExecutionContext
) extends Logging {

  def getMergeRequestId(module: UUID): Future[Option[MergeRequestId]] =
    repo.getMergeRequestId(module)

  def getByModuleOpt(moduleId: UUID): Future[Option[ModuleDraft]] =
    repo.getByModuleOpt(moduleId)

  def createNew(
      protocol: ModuleProtocol,
      person: Identity.Person,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, ModuleDraft]] = {
    given CorrelationId = CorrelationId.random()
    val updatedKeys     = nonEmptyKeys(protocol)
    val action          =
      if (updatedKeys.isEmpty) Future.failed(new Exception("no changes to the module could be found"))
      else
        create(
          protocol,
          ModuleDraftSource.Added,
          versionScheme,
          UUID.randomUUID(),
          person,
          updatedKeys
        )
    action.andThen {
      case Success(Right(d)) =>
        logger.infoC(s"module draft created module=${d.module} title=${d.moduleTitle} actor=${person.id}")
      case Failure(e) =>
        logger.errorC(s"module draft create failed actor=${person.id}", e)
      case _ => ()
    }
  }

  def delete(moduleId: UUID): Future[Unit] = {
    given CorrelationId = CorrelationId.random()
    val action          = for {
      _ <- gitBranchService.deleteModuleBranch(moduleId)
      _ <- repo.delete(moduleId).map(_ => ())
    } yield logger.infoC(s"module draft deleted module=$moduleId")
    action.andThen {
      case Failure(e) =>
        logger.errorC(s"module draft delete failed module=$moduleId", e)
      case _ => ()
    }
  }

  def createOrUpdate(request: ModuleUpdateRequest): Future[Either[PipelineError, Unit]] =
    repo
      .hasModuleDraft(request.moduleId)
      .flatMap { hasDraft =>
        given CorrelationId = CorrelationId.random()
        val (label, action) =
          if hasDraft then ("updated", update(request))
          else ("created_from_existing", createFromExistingModule(request))
        action
          .map(_.map { _ =>
            logger.infoC(
              s"module draft $label module=${request.moduleId} title=${request.protocol.metadata.title} actor=${request.person.id}"
            )
          })
          .andThen {
            case Failure(e) =>
              logger.errorC(
                s"module draft create or update failed module=${request.moduleId} actor=${request.person.id}",
                e
              )
            case _ => ()
          }
      }

  private def getFromStaging(id: UUID) =
    gitFileDownloadService.downloadModuleFromPreviewBranch(id)

  private def createFromExistingModule(request: ModuleUpdateRequest): Future[Either[PipelineError, Unit]] =
    for {
      module <- getFromStaging(request.moduleId)
        .continueIf(_.isDefined, s"file for module ${request.moduleId} does not existing in git")
      (_, modifiedKeys) = diff(
        module.get.normalize(),
        request.protocol.normalize(),
        None,
        Set.empty
      )
      res <-
        if (modifiedKeys.isEmpty) Future.successful(Right(()))
        else
          create(
            request.protocol,
            ModuleDraftSource.Modified,
            request.versionScheme,
            request.moduleId,
            request.person,
            modifiedKeys
          ).map(_.map(_ => ()))
    } yield res

  private def update(request: ModuleUpdateRequest): Future[Either[PipelineError, Unit]] =
    for {
      draft <- repo
        .getByModule(request.moduleId)
        .continueIf(draft => canEdit(draft.state(), request.canApproveModule), "can't edit module")
      origin <- getFromStaging(draft.module)
      existing                = draft.protocol()
      (updated, modifiedKeys) = diff(
        existing.normalize(),
        request.protocol.normalize(),
        origin,
        draft.modifiedKeys
      )
      res <-
        if (modifiedKeys.isEmpty) delete(request.moduleId).map(Right.apply)
        else
          for {
            res <- pipeline.printParseValidate(updated, request.versionScheme, request.moduleId)
            res <- res match {
              case Left(err)              => Future.successful(Left(err))
              case Right((module, print)) =>
                for {
                  commitId <- gitCommitService.commit(
                    draft.branch,
                    request.person,
                    commitMessage(modifiedKeys -- draft.modifiedKeys),
                    draft.module,
                    print
                  )
                  _ <- repo.updateDraft(
                    request.moduleId,
                    module.metadata.title,
                    module.metadata.abbrev,
                    toJson(updated),
                    toJson(module),
                    print,
                    keysToBeReviewed(modifiedKeys),
                    modifiedKeys,
                    commitId
                  )
                  updatedDraft <- repo.getByModule(request.moduleId)
                  _            <-
                    if shouldClearMergeRequest(updatedDraft.state()) then
                      repo.updateMergeRequest(request.moduleId, None)
                    else Future.unit
                } yield Right(())
            }
          } yield res
    } yield res

  private def commitMessage(updatedKeys: Set[String]) =
    s"updated keys: ${updatedKeys.mkString(", ")}"

  private def toJson(module: Module) =
    Json.toJson(module.normalized())

  private def toJson(protocol: ModuleProtocol) =
    Json.toJson(protocol.normalize())

  private def create(
      protocol: ModuleProtocol,
      status: ModuleDraftSource,
      versionScheme: VersionScheme,
      moduleId: UUID,
      person: Identity.Person,
      updatedKeys: Set[String]
  ) =
    pipeline.printParseValidate(protocol, versionScheme, moduleId).flatMap {
      case Left(err)              => Future.successful(Left(err))
      case Right((module, print)) =>
        val commitMsg =
          if (status.isAdded) "new module"
          else commitMessage(updatedKeys)
        for {
          branch   <- gitBranchService.createModuleBranch(moduleId)
          commitId <- gitCommitService.commit(
            branch,
            person,
            commitMsg,
            moduleId,
            print
          )
          moduleDraft = ModuleDraft(
            moduleId,
            module.metadata.title,
            module.metadata.abbrev,
            person.id,
            branch,
            status,
            toJson(protocol),
            toJson(module),
            print,
            keysToBeReviewed(updatedKeys),
            updatedKeys,
            Some(commitId),
            None,
            LocalDateTime.now()
          )
          created <- repo.create(moduleDraft)
        } yield Right(created)
    }

  private def keysToBeReviewed(updatedKeys: Set[String]): Set[String] =
    updatedKeys.filter(keysToReview.contains)

  private def canEdit(state: ModuleDraftState, canApproveModule: Boolean): Boolean = {
    val canEditByState = state match {
      case ModuleDraftState.Published | ModuleDraftState.ValidForReview | ModuleDraftState.ValidForPublication |
          ModuleDraftState.WaitingForChanges =>
        true
      case ModuleDraftState.WaitingForReview | ModuleDraftState.Unknown | ModuleDraftState.WaitingForPublication =>
        false
    }
    canEditByState || (state == ModuleDraftState.WaitingForReview && canApproveModule)
  }

  private def shouldClearMergeRequest(state: ModuleDraftState): Boolean =
    state == ModuleDraftState.WaitingForChanges
}
