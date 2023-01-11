package service

import parsing.types.ParsedMetadata
import validator.{Metadata, MetadataValidator, Module}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait MetadataValidatorService {
  def validate(metadata: ParsedMetadata): Future[Metadata]
}

@Singleton
final class MetadataValidatorServiceImpl @Inject() (
    val service: MetadataService,
    private implicit val ctx: ExecutionContext
) extends MetadataValidatorService {
  override def validate(metadata: ParsedMetadata) =
    for {
      idsAndAbbrevs <- service
        .allIdsAndAbbrevs() // TODO add current ParsedMetadata if a batch of file is processed
      modules = idsAndAbbrevs.map(Module.tupled)
      metadata <- MetadataValidator
        .validate(
          Seq(metadata),
          30, // TODO use from StudyProgram
          a => modules.find(_.abbrev.toLowerCase == a.toLowerCase)
        )
        .head
        .fold(
          errs =>
            Future.failed(
              new Throwable(
                s"Validation failed with Errors: ${errs.mkString("\n")}"
              )
            ),
          Future.successful
        )
    } yield metadata
}
