package service

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Success

import database.repo.ModuleDraftRepository
import git.api.GitBranchService
import git.api.GitCommitService
import git.api.GitFileDownloadService
import git.MergeRequestId
import models.*
import models.core.Identity
import ops.FutureOps.Ops
import parsing.metadata.VersionScheme
import parsing.types.*
import play.api.libs.json.*
import play.api.Logging
import service.modulediff.ModuleProtocolDiff.diff
import service.modulediff.ModuleProtocolDiff.nonEmptyKeys

@Singleton
final class ModuleDraftService @Inject() (
    private val moduleDraftRepository: ModuleDraftRepository,
    private val gitBranchService: GitBranchService,
    private val gitCommitService: GitCommitService,
    private val keysToReview: ModuleKeysToReview,
    private val gitFileDownloadService: GitFileDownloadService,
    private val pipeline: MetadataPipeline,
    private implicit val ctx: ExecutionContext
) extends Logging {

  def getMergeRequestId(module: UUID): Future[Option[MergeRequestId]] =
    moduleDraftRepository.getMergeRequestId(module)

  def getByModule(moduleId: UUID): Future[ModuleDraft] =
    moduleDraftRepository.getByModule(moduleId)

  def getByModuleOpt(moduleId: UUID): Future[Option[ModuleDraft]] =
    moduleDraftRepository.getByModuleOpt(moduleId)

  def isAuthorOf(moduleId: UUID, personId: String) =
    moduleDraftRepository.isAuthorOf(moduleId, personId)

  def createNew(
      protocol: ModuleProtocol,
      person: Identity.Person,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, ModuleDraft]] = {
    val updatedKeys = nonEmptyKeys(protocol)
    if (updatedKeys.isEmpty) abortNoChanges
    else
      create(
        protocol,
        ModuleDraftSource.Added,
        versionScheme,
        UUID.randomUUID(),
        person,
        updatedKeys
      ).andThen {
        case Success(Right(d)) => logger.info(s"Successfully created module draft ${d.module} (${d.moduleTitle})")
      }
  }

  def delete(moduleId: UUID): Future[Unit] =
    for {
      _ <- gitBranchService.deleteModuleBranch(moduleId)
      _ <- moduleDraftRepository.delete(moduleId).map(_ => ())
    } yield logger.info(s"Successfully deleted module draft $moduleId")

  def createOrUpdate(
      moduleId: UUID,
      protocol: ModuleProtocol,
      person: Identity.Person,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, Unit]] =
    moduleDraftRepository
      .hasModuleDraft(moduleId)
      .flatMap(hasDraft => {
        if (hasDraft)
          update(moduleId, protocol, person, versionScheme)
            .map(_.map(_ => logger.info(s"Successfully updated module draft $moduleId (${protocol.metadata.title})")))
        else
          createFromExistingModule(
            moduleId,
            protocol,
            person,
            versionScheme
          ).map(_.map(_ => logger.info(s"Successfully created module draft $moduleId (${protocol.metadata.title})")))
      })

  private def abortNoChanges =
    Future.failed(new Exception("no changes to the module could be found"))

  private def getFromStaging(uuid: UUID) =
    gitFileDownloadService.downloadModuleFromPreviewBranch(uuid).map(_.map(_._1))

  private def createFromExistingModule(
      moduleId: UUID,
      protocol: ModuleProtocol,
      person: Identity.Person,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, ModuleDraft]] =
    for {
      module <- getFromStaging(moduleId)
        .continueIf(
          _.isDefined,
          s"file for module $moduleId does not existing in git"
        )
      (_, modifiedKeys) = diff(
        module.get.normalize(),
        protocol.normalize(),
        None,
        Set.empty
      )
      res <-
        if (modifiedKeys.isEmpty) abortNoChanges
        else
          create(
            protocol,
            ModuleDraftSource.Modified,
            versionScheme,
            moduleId,
            person,
            modifiedKeys
          )
    } yield res

  private def update(
      moduleId: UUID,
      protocol: ModuleProtocol,
      person: Identity.Person,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, Unit]] =
    for {
      draft <- moduleDraftRepository
        .getByModule(moduleId)
        .continueIf(_.state().canEdit, "can't edit module")
      origin <- getFromStaging(draft.module)
      existing = draft.protocol()
      (updated, modifiedKeys) = diff(
        existing.normalize(),
        protocol.normalize(),
        origin,
        draft.modifiedKeys
      )
      res <-
        if (modifiedKeys.isEmpty) {
          delete(moduleId).map(Right(_))
        } else {
          /*
TODO this check causes a problem when a merged key is modified because it doesn't change the hashset of changed keys
           */
          //      res <-
          //        if (modifiedKeys.removedAll(draft.modifiedKeys).isEmpty) abortNoChanges
          //        else pipeline.printParseValidate(updated, versionScheme, moduleId)
          for {
            res <- pipeline.printParseValidate(updated, versionScheme, moduleId)
            res <- res match {
              case Left(err) => Future.successful(Left(err))
              case Right((module, print)) =>
                for {
                  commitId <- gitCommitService.commit(
                    draft.branch,
                    person,
                    commitMessage(modifiedKeys -- draft.modifiedKeys),
                    draft.module,
                    print
                  )
                  _ <- moduleDraftRepository.updateDraft(
                    moduleId,
                    module.metadata.title,
                    module.metadata.abbrev,
                    toJson(updated),
                    toJson(module),
                    print,
                    keysToBeReviewed(modifiedKeys),
                    modifiedKeys,
                    commitId,
                    None
                  )
                } yield Right(())
            }
          } yield res
        }
    } yield res

  private def commitMessage(updatedKeys: Set[String]) =
    s"updated keys: ${updatedKeys.mkString(", ")}"

  private def toJson(module: Module) =
    Json.toJson(module.normalize())

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
      case Left(err) => Future.successful(Left(err))
      case Right((module, print)) =>
        val commitMsg =
          if (status.isAdded) "new module"
          else commitMessage(updatedKeys)
        for {
          branch <- gitBranchService.createModuleBranch(moduleId)
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
          created <- moduleDraftRepository.create(moduleDraft)
        } yield Right(created)
    }

  private def keysToBeReviewed(updatedKeys: Set[String]): Set[String] =
    updatedKeys.filter(keysToReview.contains)
}
