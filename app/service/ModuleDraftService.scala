package service

import controllers.formats.{
  ModuleCompendiumFormat,
  ModuleCompendiumProtocolFormat,
  PipelineErrorFormat
}
import database.ModuleCompendiumOutput
import database.repo.ModuleDraftRepository
import git.api.{GitBranchService, GitCommitService, GitFileDownloadService}
import models._
import models.core.Person
import ops.EitherOps.EOps
import ops.FutureOps.Ops
import parsing.metadata.VersionScheme
import parsing.types._
import play.api.Logging
import play.api.libs.json._
import printing.yaml.ModuleCompendiumYamlPrinter
import service.ModuleCompendiumProtocolDeltaUpdate.{deltaUpdate, nonEmptyKeys}
import validator.{Metadata, ValidationError}

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ModuleDraftService {
  def getByModule(moduleId: UUID): Future[ModuleDraft]

  def getByModuleOpt(moduleId: UUID): Future[Option[ModuleDraft]]

  def hasModuleDraft(moduleId: UUID): Future[Boolean]

  def isAuthorOf(moduleId: UUID, personId: String): Future[Boolean]

  def allByPerson(personId: String): Future[Seq[ModuleDraft]]

  def createNew(
      protocol: ModuleCompendiumProtocol,
      person: Person.Default,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, ModuleDraft]]

  def delete(moduleId: UUID): Future[Unit]

  def createOrUpdate(
      moduleId: UUID,
      protocol: ModuleCompendiumProtocol,
      person: Person.Default,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, Unit]]
}

@Singleton
final class ModuleDraftServiceImpl @Inject() (
    private val moduleDraftRepository: ModuleDraftRepository,
    private val moduleCompendiumPrinter: ModuleCompendiumYamlPrinter,
    private val metadataParsingService: MetadataParsingService,
    private val moduleCompendiumService: ModuleCompendiumService,
    private val gitBranchService: GitBranchService,
    private val gitCommitService: GitCommitService,
    private val keysToReview: ModuleKeysToReview,
    private val gitFileDownloadService: GitFileDownloadService,
    private implicit val ctx: ExecutionContext
) extends ModuleDraftService
    with ModuleCompendiumProtocolFormat
    with PipelineErrorFormat
    with ModuleCompendiumFormat
    with Logging {

  override def getByModule(moduleId: UUID): Future[ModuleDraft] =
    moduleDraftRepository.getByModule(moduleId)

  override def getByModuleOpt(moduleId: UUID) =
    moduleDraftRepository.getByModuleOpt(moduleId)

  override def hasModuleDraft(moduleId: UUID) =
    moduleDraftRepository.hasModuleDraft(moduleId)

  def allByPerson(personId: String): Future[Seq[ModuleDraft]] =
    moduleDraftRepository.allByAuthor(personId)

  override def isAuthorOf(moduleId: UUID, personId: String) =
    moduleDraftRepository.isAuthorOf(moduleId, personId)

  override def createNew(
      protocol: ModuleCompendiumProtocol,
      person: Person.Default,
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
      )
  }

  def delete(moduleId: UUID): Future[Unit] =
    for {
      _ <- gitBranchService.deleteBranch(moduleId)
      _ <- moduleDraftRepository.delete(moduleId).map(_ => ())
    } yield ()

  override def createOrUpdate(
      moduleId: UUID,
      protocol: ModuleCompendiumProtocol,
      person: Person.Default,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, Unit]] =
    moduleDraftRepository
      .hasModuleDraft(moduleId)
      .flatMap(hasDraft => {
        if (hasDraft)
          update(moduleId, protocol, person, versionScheme)
        else
          createFromExistingModule(
            moduleId,
            protocol,
            person,
            versionScheme
          ).map(_.map(_ => ()))
      })

  private def abortNoChanges =
    Future.failed(new Throwable("no changes to the module could be found"))

  private def getFromStaging(uuid: UUID) =
    gitFileDownloadService.downloadModuleFromDraftBranch(uuid)

  private def createFromExistingModule(
      moduleId: UUID,
      protocol: ModuleCompendiumProtocol,
      person: Person.Default,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, ModuleDraft]] =
    for {
      mc <- getFromStaging(moduleId)
        .continueIf(
          _.isDefined,
          s"file for module $moduleId does not existing in git"
        )
      (_, modifiedKeys) = deltaUpdate(
        toProtocol(mc.get).normalize(),
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
      protocol: ModuleCompendiumProtocol,
      person: Person.Default,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, Unit]] =
    for {
      draft <- moduleDraftRepository
        .getByModule(moduleId)
        .continueIf(_.state().canEdit, "can't edit module")
      origin <- getFromStaging(draft.module)
      existing = draft.protocol()
      (updated, modifiedKeys) = deltaUpdate(
        existing.normalize(),
        protocol.normalize(),
        origin,
        draft.modifiedKeys
      )
      res <-
        if (modifiedKeys.removedAll(draft.modifiedKeys).isEmpty) abortNoChanges
        else pipeline(updated, versionScheme, moduleId)
      res <- res match {
        case Left(err) => Future.successful(Left(err))
        case Right((mc, print)) =>
          for {
            commitId <- gitCommitService.commit(
              draft.branch,
              person,
              commitMessage(modifiedKeys -- draft.modifiedKeys),
              draft.module,
              draft.source,
              print
            )
            _ <- moduleDraftRepository.updateDraft(
              moduleId,
              mc.metadata.title,
              mc.metadata.abbrev,
              toJson(updated),
              toJson(mc),
              print,
              keysToBeReviewed(modifiedKeys),
              modifiedKeys,
              commitId,
              None
            )
          } yield Right(())
      }
    } yield res

  private def commitMessage(updatedKeys: Set[String]) =
    s"updated keys: ${updatedKeys.mkString(", ")}"

  private def toJson(mc: ModuleCompendium) =
    Json.toJson(mc.normalize())

  private def toJson(protocol: ModuleCompendiumProtocol) =
    Json.toJson(protocol.normalize())

  private def toProtocol(
      mcOutput: ModuleCompendiumOutput
  ): ModuleCompendiumProtocol =
    ModuleCompendiumProtocol(
      MetadataProtocol(
        mcOutput.metadata.title,
        mcOutput.metadata.abbrev,
        mcOutput.metadata.moduleType,
        mcOutput.metadata.ects,
        mcOutput.metadata.language,
        mcOutput.metadata.duration,
        mcOutput.metadata.season,
        ParsedWorkload(
          mcOutput.metadata.workload.lecture,
          mcOutput.metadata.workload.seminar,
          mcOutput.metadata.workload.practical,
          mcOutput.metadata.workload.exercise,
          mcOutput.metadata.workload.projectSupervision,
          mcOutput.metadata.workload.projectWork
        ),
        mcOutput.metadata.status,
        mcOutput.metadata.location,
        mcOutput.metadata.participants,
        mcOutput.metadata.moduleRelation,
        mcOutput.metadata.moduleManagement,
        mcOutput.metadata.lecturers,
        mcOutput.metadata.assessmentMethods,
        mcOutput.metadata.prerequisites,
        mcOutput.metadata.po,
        mcOutput.metadata.competences,
        mcOutput.metadata.globalCriteria,
        mcOutput.metadata.taughtWith
      ),
      mcOutput.deContent,
      mcOutput.enContent
    )

  private def create(
      protocol: ModuleCompendiumProtocol,
      status: ModuleDraftSource,
      versionScheme: VersionScheme,
      moduleId: UUID,
      person: Person.Default,
      updatedKeys: Set[String]
  ) =
    pipeline(protocol, versionScheme, moduleId).flatMap {
      case Left(err) => Future.successful(Left(err))
      case Right((mc, print)) =>
        val commitMsg =
          if (status == ModuleDraftSource.Added) "new module"
          else commitMessage(updatedKeys)
        for {
          branch <- gitBranchService.createBranch(moduleId)
          commitId <- gitCommitService.commit(
            branch,
            person,
            commitMsg,
            moduleId,
            status,
            print
          )
          moduleDraft = ModuleDraft(
            moduleId,
            mc.metadata.title,
            mc.metadata.abbrev,
            person.id,
            branch,
            status,
            toJson(protocol),
            toJson(mc),
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

  private def pipeline(
      protocol: ModuleCompendiumProtocol,
      versionScheme: VersionScheme,
      moduleId: UUID
  ): Future[Either[PipelineError, (ModuleCompendium, Print)]] = {
    def print(): Either[PipelineError, Print] =
      moduleCompendiumPrinter
        .print(versionScheme, moduleId, protocol)
        .bimap(
          PipelineError.Printer(_, Some(moduleId)),
          Print.apply
        )

    def parse(
        print: Print
    ): Future[Either[PipelineError, (ParsedMetadata, Content, Content)]] =
      metadataParsingService
        .parse(print)
        .map(_.bimap(PipelineError.Parser(_, Some(moduleId)), identity))

    def validate(
        metadata: ParsedMetadata
    ): Future[Either[PipelineError, Metadata]] =
      moduleCompendiumService.allModules(Map.empty).map { existing =>
        MetadataValidatingService
          .validate(existing, metadata)
          .bimap(
            errs =>
              PipelineError
                .Validator(ValidationError(errs), Some(metadata.id)),
            identity
          )
      }

    for {
      parsed <- continueWith(print())(parse)
      validated <- continueWith(parsed)(a => validate(a._2._1))
    } yield validated.map(t =>
      (ModuleCompendium(t._2, t._1._2._2, t._1._2._3), t._1._1)
    )
  }
}
