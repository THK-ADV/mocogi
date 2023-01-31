package service

import controllers.formats.{
  ModuleCompendiumFormat,
  ModuleCompendiumProtocolFormat,
  PipelineErrorFormat
}
import database.repo.{ModuleDraftRepository, UserBranchRepository}
import models.{ModuleCompendiumProtocol, ModuleDraft, ModuleDraftStatus}
import parser.ParsingError
import parsing.metadata.VersionScheme
import parsing.types._
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}
import printer.PrintingError
import printing.ModuleCompendiumProtocolPrinter
import validator.{MetadataValidator, Module, ValidationError}

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

sealed trait PipelineError {
  def metadata: UUID
}

object PipelineError {
  case class Parser(error: ParsingError, metadata: UUID) extends PipelineError
  case class Printer(error: PrintingError, metadata: UUID) extends PipelineError
  case class Validator(error: ValidationError, metadata: UUID)
      extends PipelineError
}

@Singleton
class ModuleDraftService @Inject() (
    private val repo: ModuleDraftRepository,
    private val userBranchRepository: UserBranchRepository,
    private val moduleCompendiumService: ModuleCompendiumService,
    private val moduleCompendiumPrinter: ModuleCompendiumProtocolPrinter,
    private val metadataParserService: MetadataParserService,
    private val moduleCompendiumContentParsing: ModuleCompendiumContentParsing,
    private implicit val ctx: ExecutionContext
) extends ModuleCompendiumProtocolFormat
    with PipelineErrorFormat
    with ModuleCompendiumFormat {
  import ops.EitherOps._

  type Print = String
  type PrintingResult =
    Future[Either[Seq[PipelineError], Seq[(UUID, Print)]]]
  type ParsingResult =
    Future[
      Either[Seq[PipelineError], Seq[(Print, ParsedMetadata, Content, Content)]]
    ]
  type ValidationResult =
    Future[Either[Seq[PipelineError], Seq[(Print, ModuleCompendium)]]]

  def allFromBranch(branch: String): Future[Seq[ModuleDraft]] =
    repo.allFromBranch(branch)

  def createOrUpdate(
      module: Option[UUID],
      data: String,
      branch: String
  ): Future[ModuleDraft] = {
    def go() = module match {
      case Some(id) =>
        repo.get(id).flatMap {
          case Some(draft) =>
            repo
              .update(draft.copy(data = data, lastModified = LocalDateTime.now))
          case None =>
            repo.create(
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
        repo.create(
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
        if (branchExists) go()
        else
          Future.failed(
            new Throwable(s"branch $branch doesn't exist")
          )
    } yield res
  }

  private def print(branch: String): PrintingResult =
    for {
      drafts <- repo.allFromBranch(branch)
      protocols <- Future.fromTry(parseDrafts(drafts))
      printer = moduleCompendiumPrinter.printer(VersionScheme(1, "s"))
      (errs, prints) = protocols.partitionMap(e =>
        printer
          .print(e, "")
          .bimap(
            PipelineError.Printer(_, e._1),
            (e._1, _)
          )
      )
    } yield Either.cond(errs.isEmpty, prints, errs)

  private def parse(inputs: Seq[(UUID, Print)]): ParsingResult =
    metadataParserService.parseMany(inputs).map { res =>
      val (errs, parses) = res.partitionMap { case (res, rest) =>
        res match {
          case Left((id, err)) => Left(PipelineError.Parser(err, id))
          case Right((print, parsedMetadata)) =>
            moduleCompendiumContentParsing.parse2(rest)._1 match {
              case Left(err) =>
                Left(PipelineError.Parser(err, parsedMetadata.id))
              case Right((de, en)) => Right((print, parsedMetadata, de, en))
            }

        }
      }
      Either.cond(errs.isEmpty, parses, errs)
    }

  private def validate(
      parsed: Seq[(Print, ParsedMetadata, Content, Content)]
  ): ValidationResult =
    moduleCompendiumService.allIdsAndAbbrevs().map { existing =>
      val existingModules = existing.map(Module.tupled)
      val parsedModules = parsed.map(a => Module(a._2.id, a._2.abbrev))
      val modules = existingModules ++ parsedModules
      val validator =
        MetadataValidator.validate(30, id => modules.find(_.id == id)) _
      val (errs, moduleCompendiums) =
        parsed.partitionMap { case (print, parsedMetadata, de, en) =>
          validator(parsedMetadata).bimap(
            errs =>
              PipelineError.Validator(
                ValidationError(errs),
                parsedMetadata.id
              ),
            metadata => (print, ModuleCompendium(metadata, de, en))
          )
        }
      Either.cond(errs.isEmpty, moduleCompendiums, errs)
    }

  private def persist(
      e: Either[Seq[PipelineError], Seq[(Print, ModuleCompendium)]]
  ) =
    Future.sequence(
      e match {
        case Left(errs) =>
          errs.groupBy(_.metadata).map { case (id, errs) =>
            repo.updateValidation(id, Left(Json.toJson(errs).toString()))
          }
        case Right(mcs) =>
          mcs.map { case (print, mc) =>
            val mcJson = Json.toJson(mc).toString()
            val printJson = JsString(print).toString()
            repo.updateValidation(mc.metadata.id, Right((mcJson, printJson)))
          }
      }
    )

  private def continue[A, B](
      e: Either[Seq[PipelineError], Seq[A]],
      f: Seq[A] => Future[Either[Seq[PipelineError], B]]
  ): Future[Either[Seq[PipelineError], B]] =
    e match {
      case Left(errs) =>
        Future.successful(Left(errs))
      case Right(res) =>
        f(res)
    }

  def validateDrafts(branch: String): ValidationResult =
    for {
      prints <- print(branch)
      parses <- continue(prints, parse)
      validates <- continue(parses, validate)
      _ <- persist(validates)
    } yield validates

  private def parseDrafts(
      drafts: Seq[ModuleDraft]
  ): Try[Seq[(UUID, ModuleCompendiumProtocol)]] =
    Try(drafts.map { draft =>
      moduleCompendiumProtocolFormat.reads(Json.parse(draft.data)) match {
        case JsSuccess(value, _) => (draft.module, value)
        case JsError(errors)     => throw new Throwable(errors.mkString("\n"))
      }
    })
}
