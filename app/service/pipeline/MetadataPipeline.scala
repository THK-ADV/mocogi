package service.pipeline

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.EitherT
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
import service.ModuleService

@Singleton
final class MetadataPipeline @Inject() (
    private val parser: MetadataParsingService,
    private val moduleService: ModuleService,
    private val moduleYamlPrinter: ModuleYamlPrinter,
    implicit val ctx: ExecutionContext
) {
  private type ParsedModule = (ParsedMetadata, ModuleContent, ModuleContent)

  def parseValidate(print: Print): Future[Module] =
    for {
      parsed   <- parser.parse(print).unwrap
      existing <- allModules()
      metadata <- validate(existing, parsed._1).toFuture
    } yield Module(metadata, parsed._2, parsed._3)

  def parseValidateMany(prints: Seq[Print]): Future[Either[Seq[PipelineError], Seq[(Print, Module)]]] = {
    val parse    = parser.parseMany(prints)
    val existing = allModules()
    for {
      parsed   <- parse
      existing <- existing
    } yield parsed match {
      case Left(value)   => Left(value)
      case Right(parsed) => MetadataValidationService.validateMany(existing, parsed)
    }
  }

  def printParseValidate(
      protocol: ModuleProtocol,
      versionScheme: VersionScheme,
      moduleId: UUID
  ): Future[Either[PipelineError, (Module, Print)]] =
    EitherT
      .fromEither[Future](print(protocol, versionScheme, moduleId))
      .flatMap(print =>
        EitherT(parse(print, moduleId))
          .flatMap(parsed => EitherT(validate(parsed._1)).map(metadata => (print, parsed, metadata)))
      )
      .map {
        case (print, (_, de, en), metadata) =>
          (Module(metadata, de, en), print)
      }
      .value

  private def allModules(): Future[Seq[ModuleCore]] =
    for {
      allFromLive  <- moduleService.allModuleCore()
      allFromDraft <- moduleService.allNewlyCreated()
    } yield allFromLive ++ allFromDraft

  private def print(
      protocol: ModuleProtocol,
      versionScheme: VersionScheme,
      moduleId: UUID
  ): Either[PipelineError, Print] =
    moduleYamlPrinter
      .print(versionScheme, moduleId, protocol)
      .bimap(PipelineError.printer(_, Some(moduleId)), Print.apply)

  private def parse(
      print: Print,
      moduleId: UUID
  ): Future[Either[PipelineError, ParsedModule]] =
    parser.parse(print).map(_.bimap(PipelineError.parser(_, Some(moduleId)), identity))

  private def validate(
      metadata: ParsedMetadata
  ): Future[Either[PipelineError, Metadata]] =
    allModules().map(validate(_, metadata))

  private def validate(
      existing: Seq[ModuleCore],
      metadata: ParsedMetadata
  ): Either[PipelineError, Metadata] =
    MetadataValidationService
      .validate(existing, metadata)
      .mapErr(errs => PipelineError.validator(errs, Some(metadata.id)))
}
