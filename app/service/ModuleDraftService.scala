package service

import controllers.formats.{
  ModuleCompendiumFormat,
  ModuleCompendiumProtocolFormat,
  PipelineErrorFormat
}
import database.ModuleCompendiumOutput
import database.repo.ModuleDraftRepository
import git.api.{GitBranchService, GitCommitService}
import models._
import ops.EitherOps.EOps
import parsing.metadata.VersionScheme
import parsing.types._
import play.api.libs.json._
import printing.yaml.ModuleCompendiumYamlPrinter
import service.ModuleCompendiumNormalizer.normalize
import validator.{Metadata, ValidationError}

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ModuleDraftService {
  def allByModules(modules: Seq[UUID]): Future[Seq[ModuleDraft]]

  def getByModule(moduleId: UUID): Future[ModuleDraft]

  def getByMergeRequest(
      mergeRequestId: MergeRequestId
  ): Future[Seq[ModuleDraft]]

  def allByUser(user: User): Future[Seq[ModuleDraft]]

  def createNew(
      protocol: ModuleCompendiumProtocol,
      user: User,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, ModuleDraft]]

  def deleteDraftWithBranch(moduleId: UUID): Future[Unit]

  def deleteDraft(moduleId: UUID): Future[Unit]

  def deleteDrafts(moduleIds: Seq[UUID]): Future[Unit]

  def createOrUpdate(
      moduleId: UUID,
      protocol: ModuleCompendiumProtocol,
      modifiedKeys: Set[String],
      user: User,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, Unit]]

  def parseModuleCompendium(json: JsValue): ModuleCompendium

  def updateMergeRequestId(
      moduleId: UUID,
      mergeRequestId: Option[MergeRequestId]
  ): Future[Unit]
}

@Singleton
final class ModuleDraftServiceImpl @Inject() (
    private val moduleDraftRepository: ModuleDraftRepository,
    private val metadataValidatingService: MetadataValidatingService,
    private val moduleCompendiumPrinter: ModuleCompendiumYamlPrinter,
    private val metadataParsingService: MetadataParsingService,
    private val moduleCompendiumService: ModuleCompendiumService,
    private val gitBranchService: GitBranchService,
    private val gitCommitService: GitCommitService,
    private val keysToReview: ModuleKeysToReview,
    private implicit val ctx: ExecutionContext
) extends ModuleDraftService
    with ModuleCompendiumProtocolFormat
    with PipelineErrorFormat
    with ModuleCompendiumFormat {

  def allByModules(modules: Seq[UUID]): Future[Seq[ModuleDraft]] =
    moduleDraftRepository.allByModules(modules)

  override def getByModule(moduleId: UUID): Future[ModuleDraft] =
    moduleDraftRepository.getByModule(moduleId)

  override def getByMergeRequest(mergeRequestId: MergeRequestId) =
    moduleDraftRepository.getByMergeRequest(mergeRequestId)

  def allByUser(user: User): Future[Seq[ModuleDraft]] =
    moduleDraftRepository.allByUser(user)

  override def updateMergeRequestId(
      moduleId: UUID,
      mergeRequestId: Option[MergeRequestId]
  ) =
    moduleDraftRepository.updateMergeRequestId(moduleId, mergeRequestId)

  def createNew(
      protocol: ModuleCompendiumProtocol,
      user: User,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, ModuleDraft]] =
    create(
      protocol,
      ModuleDraftSource.Added,
      versionScheme,
      UUID.randomUUID(),
      user,
      Set.empty
    )

  def deleteDraftWithBranch(moduleId: UUID): Future[Unit] =
    for {
      _ <- gitBranchService.deleteBranch(moduleId)
      _ <- deleteDraft(moduleId)
    } yield ()

  override def deleteDraft(moduleId: UUID) =
    moduleDraftRepository.delete(moduleId).map(_ => ())

  override def deleteDrafts(moduleIds: Seq[UUID]) =
    moduleDraftRepository.deleteDrafts(moduleIds).map(_ => ())

  override def createOrUpdate(
      moduleId: UUID,
      protocol: ModuleCompendiumProtocol,
      modifiedKeys: Set[String],
      user: User,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, Unit]] =
    moduleDraftRepository
      .hasModuleDraft(moduleId)
      .flatMap(hasDraft =>
        if (hasDraft)
          update(moduleId, protocol, modifiedKeys, user, versionScheme)
        else
          createFromExistingModule(
            moduleId,
            protocol,
            modifiedKeys,
            user,
            versionScheme
          ).map(_.map(_ => ()))
      )

  private def createFromExistingModule(
      moduleId: UUID,
      protocol: ModuleCompendiumProtocol,
      modifiedKeys: Set[String],
      user: User,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, ModuleDraft]] =
    create(
      protocol,
      ModuleDraftSource.Modified,
      versionScheme,
      moduleId,
      user,
      modifiedKeys
    )

  private def update(
      moduleId: UUID,
      protocol: ModuleCompendiumProtocol,
      modifiedKeys: Set[String],
      user: User,
      versionScheme: VersionScheme
  ): Future[Either[PipelineError, Unit]] =
    for {
      draft <- moduleDraftRepository.getByModule(moduleId)
      origin <- moduleCompendiumService.getOrNull(draft.module)
      existing = draft.protocol()
      (updated, keysToRemove) = deltaUpdate(
        existing,
        protocol,
        origin,
        modifiedKeys
      )
      res <- pipeline(updated, versionScheme, moduleId)
      res <- res match {
        case Left(err) => Future.successful(Left(err))
        case Right((mc, print)) =>
          val mergedKeysToBeReviewed =
            (draft.keysToBeReviewed ++ keysToBeReviewed(
              draft.source,
              modifiedKeys
            )) --
              keysToRemove
          val mergedModifiedKeys =
            (draft.modifiedKeys ++ modifiedKeys) -- keysToRemove
          for {
            commitId <- gitCommitService.commit(
              draft.branch,
              user,
              commitMessage(modifiedKeys),
              draft.module,
              draft.source,
              print
            )
            _ <- moduleDraftRepository.updateDraft(
              moduleId,
              toJson(updated),
              toJson(mc),
              print,
              mergedKeysToBeReviewed,
              mergedModifiedKeys,
              commitId
            )
          } yield Right(())
      }
    } yield res

  private def commitMessage(updatedKeys: Set[String]) =
    s"updated keys: ${updatedKeys.mkString(", ")}"

  private def toJson(mc: ModuleCompendium) =
    Json.toJson(normalize(mc))

  private def toJson(protocol: ModuleCompendiumProtocol) =
    Json.toJson(normalize(protocol))

  private def deltaUpdate(
      existing: ModuleCompendiumProtocol,
      newP: ModuleCompendiumProtocol,
      origin: Option[ModuleCompendiumOutput],
      modifiedKeys: Set[String]
  ): (ModuleCompendiumProtocol, Set[String]) =
    modifiedKeys.foldLeft((existing, Set.empty[String])) {
      case ((acc, keysToRemove), key) =>
        key match { // TODO unify keys and extend this implementation
          case "title" =>
            val newAcc = acc.copy(metadata =
              acc.metadata.copy(title = newP.metadata.title)
            )
            if (origin.exists(_.metadata.title == newAcc.metadata.title)) {
              (newAcc, keysToRemove + key)
            } else {
              (newAcc, keysToRemove)
            }
          case "particularities-content-de" =>
            val newAcc = acc.copy(deContent =
              acc.deContent.copy(particularities =
                newP.deContent.particularities
              )
            )
            if (
              origin.exists(
                _.deContent.particularities == newAcc.deContent.particularities
              )
            ) {
              (newAcc, keysToRemove + key)
            } else {
              (newAcc, keysToRemove)
            }
          case "particularities-content-en" =>
            val newAcc = acc.copy(enContent =
              acc.enContent.copy(particularities =
                newP.enContent.particularities
              )
            )
            if (
              origin.exists(
                _.enContent.particularities == newAcc.enContent.particularities
              )
            ) {
              (newAcc, keysToRemove + key)
            } else {
              (newAcc, keysToRemove)
            }
          case "literature-content-en" =>
            val newAcc = acc.copy(enContent =
              acc.enContent.copy(recommendedReading =
                newP.enContent.recommendedReading
              )
            )
            if (
              origin.exists(
                _.enContent.recommendedReading == newAcc.enContent.recommendedReading
              )
            ) {
              (newAcc, keysToRemove + key)
            } else {
              (newAcc, keysToRemove)
            }
          case _ => throw new Throwable(s"unsupported key $key")
        }
    }

  private def create(
      protocol: ModuleCompendiumProtocol,
      status: ModuleDraftSource,
      versionScheme: VersionScheme,
      moduleId: UUID,
      user: User,
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
            user,
            commitMsg,
            moduleId,
            status,
            print
          )
          moduleDraft = ModuleDraft(
            moduleId,
            user,
            branch,
            status,
            toJson(protocol),
            toJson(mc),
            print,
            keysToBeReviewed(status, updatedKeys),
            updatedKeys,
            Some(commitId),
            None,
            LocalDateTime.now()
          )
          created <- moduleDraftRepository.create(moduleDraft)
        } yield Right(created)
    }

  private def keysToBeReviewed(
      source: ModuleDraftSource,
      updatedKeys: Set[String]
  ): Set[String] =
    if (source == ModuleDraftSource.Added) Set.empty
    else updatedKeys.filter(keysToReview.contains)

  private def pipeline(
      protocol: ModuleCompendiumProtocol,
      versionScheme: VersionScheme,
      moduleId: UUID
  ): Future[Either[PipelineError, (ModuleCompendium, Print)]] = {
    def print(): Either[PipelineError, Print] =
      moduleCompendiumPrinter
        .printer(versionScheme)
        .print((moduleId, protocol), "")
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
      metadataValidatingService
        .validate(metadata)
        .map(
          _.bimap(
            errs =>
              PipelineError.Validator(ValidationError(errs), Some(metadata.id)),
            identity
          )
        )
    for {
      parsed <- continueWith(print())(parse)
      validated <- continueWith(parsed)(a => validate(a._2._1))
    } yield validated.map(t =>
      (ModuleCompendium(t._2, t._1._2._2, t._1._2._3), t._1._1)
    )
  }

  def parseModuleCompendium(json: JsValue): ModuleCompendium =
    Json.fromJson[ModuleCompendium](json).get
}
