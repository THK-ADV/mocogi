package service

import controllers.formats.{
  ModuleCompendiumFormat,
  ModuleCompendiumProtocolFormat,
  PipelineErrorFormat
}
import database.repo.{ModuleDraftRepository, UserBranchRepository}
import models._
import parsing.metadata.VersionScheme
import parsing.types._
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import printing.yaml.ModuleCompendiumYamlPrinter
import service.ModuleCompendiumNormalizer.normalize

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ModuleDraftService @Inject() (
    private val moduleDraftRepository: ModuleDraftRepository,
    private val userBranchRepository: UserBranchRepository,
    private val metadataValidatingService: MetadataValidatingService,
    private val moduleCompendiumPrinter: ModuleCompendiumYamlPrinter,
    private val metadataParsingService: MetadataParsingService,
    private implicit val ctx: ExecutionContext
) extends ModuleCompendiumProtocolFormat
    with PipelineErrorFormat
    with ModuleCompendiumFormat {
  import ops.EitherOps._

  def allFromBranch(branch: String): Future[Seq[ModuleDraft]] =
    moduleDraftRepository.allFromBranch(branch)

  def delete(userBranch: UserBranch): Future[Int] =
    moduleDraftRepository.delete(userBranch.branch)

  def createOrUpdate(
      module: Option[UUID],
      data: ModuleCompendiumProtocol,
      branch: String
  ): Future[ModuleDraft] = {
    def go(data: String) = module match {
      case Some(id) =>
        moduleDraftRepository.get(id).flatMap {
          case Some(draft) =>
            moduleDraftRepository
              .update(draft.copy(data = data, lastModified = LocalDateTime.now))
          case None =>
            moduleDraftRepository.create(
              ModuleDraft(
                id,
                data,
                branch,
                ModuleDraftStatus.Modified,
                LocalDateTime.now(),
                None
              )
            )
        }
      case None =>
        moduleDraftRepository.create(
          ModuleDraft(
            UUID.randomUUID,
            data,
            branch,
            ModuleDraftStatus.Added,
            LocalDateTime.now(),
            None
          )
        )
    }

    for {
      branchExists <- userBranchRepository.exists(branch)
      res <-
        if (branchExists) go(toJson(normalize(data)))
        else
          Future.failed(
            new Throwable(s"branch $branch doesn't exist")
          )
    } yield res
  }

  private def toJson(protocol: ModuleCompendiumProtocol) =
    moduleCompendiumProtocolFormat.writes(protocol).toString()

  private def fromJson(json: String) =
    moduleCompendiumProtocolFormat.reads(Json.parse(json))

  private def print(branch: String): PrintingResult =
    for {
      drafts <- moduleDraftRepository.allFromBranch(branch)
      protocols <- Future.fromTry(parseDrafts(drafts))
      printer = moduleCompendiumPrinter.printer(VersionScheme(1, "s"))
      (errs, prints) = protocols.partitionMap(e =>
        printer
          .print(e, "")
          .bimap(
            PipelineError.Printer(_, Some(e._1)),
            p => (e._1, Print(p))
          )
      )
    } yield Either.cond(errs.isEmpty, prints, errs)

  private def parse(inputs: Seq[(UUID, Print)]): ParsingResult =
    metadataParsingService.parse(inputs.map { case (id, p) => (Some(id), p) })

  private def validate(
      parsed: Seq[(Print, ParsedMetadata, Content, Content)]
  ): ValidationResult =
    metadataValidatingService.validate(parsed)

  private def persist(
      e: Either[Seq[PipelineError], Seq[(Print, ModuleCompendium)]]
  ): Future[Unit] =
    Future
      .sequence(
        e match {
          case Left(errs) =>
            errs.groupBy(_.metadata).map { case (id, errs) =>
              moduleDraftRepository
                .updateValidation(id.get, Left(Json.toJson(errs)))
            }
          case Right(mcs) =>
            mcs.map { case (print, mc) =>
              val mcJson = Json.toJson(normalize(mc))
              moduleDraftRepository.updateValidation(
                mc.metadata.id,
                Right((mcJson, print))
              )
            }
        }
      )
      .map(_ => ())

  def validateDrafts(branch: String): ValidationResult =
    for {
      prints <- print(branch)
      parses <- continue(prints, parse)
      validates <- continue(parses, validate)
      _ <- persist(validates)
    } yield validates

  def validDrafts(branch: String): Future[Seq[ValidModuleDraft]] =
    for {
      drafts <- moduleDraftRepository.allFromBranch(branch)
    } yield drafts.map { d =>
      d.validation match {
        case Some(Right((json, print))) =>
          ValidModuleDraft(
            d.module,
            d.status,
            d.lastModified,
            json,
            print
          )
        case _ =>
          throw new Throwable(
            s"expected branch $branch to only contain valid drafts"
          )
      }
    }

  def parseModuleCompendium(json: JsValue): ModuleCompendium =
    Json.fromJson[ModuleCompendium](json).get

  private def parseDrafts(
      drafts: Seq[ModuleDraft]
  ): Try[Seq[(UUID, ModuleCompendiumProtocol)]] =
    Try(drafts.map { draft =>
      fromJson(draft.data) match {
        case JsSuccess(value, _) => (draft.module, value)
        case JsError(errors)     => throw new Throwable(errors.mkString("\n"))
      }
    })

}
