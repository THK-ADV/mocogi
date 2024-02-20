package service

import models._
import ops.EitherOps.{EOps, EThrowableOps}
import ops.FutureOps.EitherOps
import parsing.metadata.VersionScheme
import parsing.types.{Module, ModuleContent, ParsedMetadata}
import printing.yaml.ModuleYamlPrinter
import validator.{Metadata, ValidationError}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class MetadataPipeline @Inject() (
    private val parser: MetadataParsingService,
    private val moduleService: ModuleService,
    private val moduleYamlPrinter: ModuleYamlPrinter,
    implicit val ctx: ExecutionContext
) {
  def parseValidate(print: Print): Future[Module] =
    for {
      (metadata, de, en) <- parser.parse(print).unwrap
      existing <- moduleService.allModuleCore(Map.empty)
      metadata <- MetadataValidatingService
        .validate(existing, metadata)
        .mapErr(errs =>
          PipelineError
            .Validator(ValidationError(errs), Some(metadata.id))
        )
        .toFuture
    } yield Module(metadata, de, en)

  def parseValidateMany(
      prints: Seq[(Option[UUID], Print)]
  ): Future[Either[Seq[PipelineError], Seq[(Print, Module)]]] =
    for {
      parsed <- parser.parseMany(prints)
      existing <- moduleService.allModuleCore(Map.empty)
    } yield parsed match {
      case Left(value) => Left(value)
      case Right(parsed) =>
        MetadataValidatingService.validateMany(existing, parsed)
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
      moduleService.allModuleCore(Map.empty).map { existing =>
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
    } yield validated.map(t => (Module(t._2, t._1._2._2, t._1._2._3), t._1._1))
  }
}
