package service

import parsing.types.ParsedMetadata
import validator.{Metadata, MetadataValidator, Module, Validation}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait MetadataValidatorService {
  def validate(metadata: ParsedMetadata): Future[Validation[Metadata]]
}

@Singleton
final class MetadataValidatorServiceImpl @Inject() (
    val service: MetadataService,
    private implicit val ctx: ExecutionContext
) extends MetadataValidatorService {
  override def validate(metadata: ParsedMetadata) =
    for {
      idsAndAbbrevs <- service.allIdsAndAbbrevs()
    } yield {
      val modules = idsAndAbbrevs.map(Module.tupled)
      MetadataValidator
        .validate(
          Seq(metadata),
          30, // TODO
          a => modules.find(_.abbrev == a)
        )
        .head
    }
}
