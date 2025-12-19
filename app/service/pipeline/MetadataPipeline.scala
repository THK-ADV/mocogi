package service.pipeline

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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
import service.*
import validation.ValidationError

@Singleton
final class MetadataPipeline @Inject() (
    private val parser: MetadataParsingService,
    private val moduleService: ModuleService,
    private val moduleYamlPrinter: ModuleYamlPrinter,
    implicit val ctx: ExecutionContext
) {
  def parseValidate(print: Print): Future[Module] = {
    val parse    = parser.parse(print).unwrap
    val existing = allModules()

    for {
      (metadata, de, en) <- parse
      existing           <- existing
      metadata           <- MetadataValidationService
        .validate(existing, metadata)
        .mapErr(errs =>
          PipelineError
            .Validator(ValidationError(errs), Some(metadata.id))
        )
        .toFuture
    } yield Module(metadata, de, en)
  }

  def parseValidateMany(
      prints: Seq[Print]
  ): Future[Either[Seq[PipelineError], Seq[(Print, Module)]]] = {
    val parse    = parser.parseMany(prints)
    val existing = allModules()

    for {
      parsed   <- parse
      existing <- existing
    } yield parsed match {
      case Left(value)   => Left(value)
      case Right(parsed) =>
        MetadataValidationService.validateMany(existing, parsed)
    }
  }

  def printParseValidate(
      protocol: ModuleProtocol,
      versionScheme: VersionScheme,
      moduleId: UUID
  ): Future[Either[PipelineError, (Module, Print)]] = {
    def print(): Either[PipelineError, Print] =
      moduleYamlPrinter
        .print(versionScheme, moduleId, protocol)
        .bimap(
          PipelineError.Printer(_, Some(moduleId)),
          Print.apply
        )

    def parse(
        print: Print
    ): Future[
      Either[PipelineError, (ParsedMetadata, ModuleContent, ModuleContent)]
    ] =
      parser
        .parse(print)
        .map(_.bimap(PipelineError.Parser(_, Some(moduleId)), identity))

    def validate(
        metadata: ParsedMetadata
    ): Future[Either[PipelineError, Metadata]] =
      allModules().map { existing =>
        MetadataValidationService
          .validate(existing, metadata)
          .bimap(
            errs =>
              PipelineError
                .Validator(ValidationError(errs), Some(metadata.id)),
            identity
          )
      }

    for {
      parsed    <- continueWith(print())(parse)
      validated <- continueWith(parsed)(a => validate(a._2._1))
    } yield validated.map(t => (Module(t._2, t._1._2._2, t._1._2._3), t._1._1))
  }

  private def allModules(): Future[Seq[ModuleCore]] =
    for
      allFromLive  <- moduleService.allModuleCore()
      allFromDraft <- moduleService.allNewlyCreated()
    yield allFromLive ++ allFromDraft
}
