package service

import controllers.formats.ModuleCompendiumProtocolFormat
import database.repo.{ModuleDraftRepository, UserBranchRepository}
import models.{ModuleCompendiumProtocol, ModuleDraft, ModuleDraftStatus}
import parser.ParsingError
import parsing.metadata.VersionScheme
import parsing.types._
import play.api.libs.json.{JsError, JsSuccess, Json}
import printer.PrintingError
import printing.ModuleCompendiumProtocolPrinter
import validator.{MetadataValidator, Module, ValidationError}

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

sealed trait PipelineError

object PipelineError {
  case class Parser(value: ParsingError) extends PipelineError
  case class Printer(value: PrintingError) extends PipelineError
  case class Validator(value: ValidationError) extends PipelineError
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
) extends ModuleCompendiumProtocolFormat {
  import ops.EitherOps._

  type PrintingResult =
    Future[Either[Seq[PipelineError], Seq[String]]]
  type ParsingResult =
    Future[Either[Seq[PipelineError], Seq[(ParsedMetadata, Content, Content)]]]
  type ValidationResult =
    Future[Either[Seq[PipelineError], Seq[ModuleCompendium]]]

  def allFromBranch(branch: String) =
    repo.allFromBranch(branch)

  def createOrUpdate(module: Option[UUID], data: String, branch: String) = {
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
                LocalDateTime.now()
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
            LocalDateTime.now()
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
        printer.print(e, "").mapLeft(PipelineError.Printer)
      )
    } yield Either.cond(errs.isEmpty, prints, errs)

  private def parse(inputs: Seq[String]): ParsingResult =
    metadataParserService.parseMany(inputs).map { res =>
      val (errs, parses) = res.partitionMap { case (res, rest) =>
        res.biflatMap(
          PipelineError.Parser,
          PipelineError.Parser,
          m =>
            moduleCompendiumContentParsing
              .parse2(rest)
              ._1
              .map(c => (m, c._1, c._2))
        )
      }
      Either.cond(errs.isEmpty, parses, errs)
    }

  private def validate(
      parsed: Seq[(ParsedMetadata, Content, Content)]
  ): ValidationResult =
    moduleCompendiumService.allIdsAndAbbrevs().map { existing =>
      val existingModules = existing.map(Module.tupled)
      val parsedModules = parsed.map(a => Module(a._1.id, a._1.abbrev))
      val modules = existingModules ++ parsedModules
      val validator =
        MetadataValidator.validate(30, id => modules.find(_.id == id)) _
      val (errs, moduleCompendiums) =
        parsed.partitionMap { case (parsedMetadata, de, en) =>
          validator(parsedMetadata).bimap(
            errs =>
              PipelineError.Validator(
                ValidationError(parsedMetadata.id, parsedMetadata.title, errs)
              ),
            metadata => ModuleCompendium(metadata, de, en)
          )
        }
      Either.cond(errs.isEmpty, moduleCompendiums, errs)
    }

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
