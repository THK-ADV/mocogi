package service.pipeline

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.EitherT
import cli.MarkdownCLI
import models.*
import ops.bimap
import ops.mapErr
import ops.toFuture
import ops.unwrap
import parsing.metadata.VersionScheme
import parsing.types.Module
import parsing.types.ModuleContent
import parsing.types.ParsedMetadata
import printing.yaml.ModuleYamlPrinter
import printer.PrintingError
import service.ModuleService
import validation.ModuleRelationGraph

@Singleton
final class MetadataPipeline @Inject() (
    private val parser: MetadataParsingService,
    private val moduleService: ModuleService,
    private val moduleYamlPrinter: ModuleYamlPrinter,
    private val markdownCLI: MarkdownCLI,
    implicit val ctx: ExecutionContext
) {
  private type ParsedModule = (ParsedMetadata, ModuleContent, ModuleContent)

  def parseValidate(print: Print): Future[Module] =
    for {
      parsed   <- parser.parse(print).unwrap
      context  <- validationContext()
      metadata <- validate(context, parsed._1).toFuture
    } yield Module(metadata, parsed._2, parsed._3)

  def parseValidateMany(prints: Seq[Print]): Future[Either[Seq[PipelineError], Seq[(Print, Module)]]] = {
    val parse   = parser.parseMany(prints)
    val context = validationContext()
    for {
      parsed  <- parse
      context <- context
    } yield parsed match {
      case Left(errors)  => Left(errors)
      case Right(parsed) =>
        MetadataValidationService.validateMany(context, parsed)
    }
  }

  def fullCheck(prints: Seq[Print]): Future[Either[Seq[PipelineError], Seq[(Print, Module)]]] = {
    val parse   = parser.parseAll(prints)
    val context = validationContext()
    for {
      (parseFailures, parsed) <- parse
      linted                  <- Future {
        val failedInputs = parseFailures.map { (print, error) => error.metadata -> print.value }
        val parsedInputs = parsed.map { (print, metadata, _, _) => Some(metadata.id) -> print.value }
        markdownCLI.lint(failedInputs ++ parsedInputs)
      }
      context <- context
    } yield {
      val lintErrors = linted.fold(
        error => Seq(PipelineError.printer(PrintingError("Markdown lint", error), None)),
        _.map { (moduleId, error) =>
          PipelineError.printer(PrintingError("Markdown lint", error), moduleId)
        }
      )
      val validation       = MetadataValidationService.validateMany(context, parsed)
      val validationErrors = validation.swap.getOrElse(Seq.empty)
      val errors           = parseFailures.map(_._2) ++ lintErrors ++ validationErrors

      Either.cond(errors.isEmpty, validation.getOrElse(Seq.empty), errors)
    }
  }

  def printParseValidate(
      protocol: ModuleProtocol,
      versionScheme: VersionScheme,
      moduleId: UUID
  ): Future[Either[PipelineError, (Module, Print)]] =
    EitherT
      .fromEither[Future](print(protocol, versionScheme, moduleId))
      .flatMap(print => EitherT(format(print, Some(moduleId))))
      .flatMap(print =>
        EitherT(parse(print, moduleId))
          .flatMap(parsed => EitherT(validate(parsed._1)).map(metadata => (print, parsed, metadata)))
      )
      .map {
        case (print, (_, de, en), metadata) =>
          (Module(metadata, de, en), print)
      }
      .value

  private def validationContext(): Future[ValidationContext] = {
    val live    = moduleService.allModuleCoreWithRelations()
    val created = moduleService.allNewlyCreated()

    for {
      (liveModules, relations) <- live
      createdModules           <- created
      modulesById = (liveModules ++ createdModules).map(module => module.id -> module).toMap
    } yield ValidationContext(modulesById, ModuleRelationGraph(relations))
  }

  private def print(
      protocol: ModuleProtocol,
      versionScheme: VersionScheme,
      moduleId: UUID
  ): Either[PipelineError, Print] =
    moduleYamlPrinter
      .print(versionScheme, moduleId, protocol)
      .bimap(PipelineError.printer(_, Some(moduleId)), Print.apply)

  private def format(print: Print, moduleId: Option[UUID]): Future[Either[PipelineError, Print]] =
    Future {
      markdownCLI
        .format(print.value)
        .bimap(
          error => PipelineError.printer(PrintingError("formatted Markdown", error), moduleId),
          Print.apply
        )
    }

  private def parse(
      print: Print,
      moduleId: UUID
  ): Future[Either[PipelineError, ParsedModule]] =
    parser.parse(print).map(_.bimap(PipelineError.parser(_, Some(moduleId)), identity))

  private def validate(
      metadata: ParsedMetadata
  ): Future[Either[PipelineError, Metadata]] =
    validationContext().map(context => validate(context, metadata))

  private def validate(
      context: ValidationContext,
      metadata: ParsedMetadata
  ): Either[PipelineError, Metadata] =
    MetadataValidationService
      .validate(context, metadata)
      .mapErr(errs => PipelineError.validator(errs, Some(metadata.id)))
}
